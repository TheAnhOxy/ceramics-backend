package com.ceramic.service.impl;

import com.ceramic.dto.BatchResponse;
import com.ceramic.dto.BatchStageHistoryDto;
import com.ceramic.dto.StageDto;
import com.ceramic.entity.Batch;
import com.ceramic.entity.BatchStageHistory;
import com.ceramic.entity.Stage;
import com.ceramic.entity.User;
import com.ceramic.enums.BatchStatus;
import com.ceramic.enums.HistoryStatus;
import com.ceramic.exception.BatchNotFoundException;
import com.ceramic.exception.InvalidStageTransitionException;
import com.ceramic.repository.BatchRepository;
import com.ceramic.repository.BatchStageHistoryRepository;
import com.ceramic.repository.StageRepository;
import com.ceramic.repository.UserRepository;
import com.ceramic.service.NotificationService;
import com.ceramic.service.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {

    private final BatchRepository batchRepo;
    private final StageRepository stageRepo;
    private final BatchStageHistoryRepository historyRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public BatchResponse advanceStage(Long batchId, Long performedBy, String note, Boolean forceSkip) {
        log.info("Chuyển công đoạn cho Mẻ gốm ID: {}, người thực hiện: {}, ghi chú: {}", batchId, performedBy, note);

        // 1. Pessimistic Lock tránh race condition khi 2 người cùng bấm chuyển trạng thái
        Batch batch = batchRepo.findByIdForUpdate(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));

        // Validation: Không cho chuyển trạng thái nếu batch đang tạm dừng hoặc thất bại hoặc đã hoàn thành
        if (batch.getStatus() == BatchStatus.ON_HOLD || batch.getStatus() == BatchStatus.FAILED) {
            throw new InvalidStageTransitionException("Không thể chuyển công đoạn vì mẻ gốm đang ở trạng thái " + batch.getStatus());
        }
        if (batch.getStatus() == BatchStatus.COMPLETED) {
            throw new InvalidStageTransitionException("Mẻ gốm đã hoàn thành toàn bộ quy trình sản xuất.");
        }

        Stage currentStage = batch.getCurrentStage();
        if (currentStage == null) {
            throw new InvalidStageTransitionException("Mẻ gốm chưa được gán công đoạn hiện tại.");
        }

        BatchStageHistory currentHistory = historyRepo.findByBatchIdAndStageId(batchId, currentStage.getId())
                .orElseThrow(() -> new InvalidStageTransitionException("Không tìm thấy lịch sử công đoạn hiện tại của mẻ gốm"));

        User performer = null;
        if (performedBy != null) {
            performer = userRepo.findById(performedBy).orElse(null);
        }

        // 2. Đóng công đoạn hiện tại
        boolean isSkipped = Boolean.TRUE.equals(forceSkip);
        currentHistory.setStatus(isSkipped ? HistoryStatus.SKIPPED : HistoryStatus.COMPLETED);
        currentHistory.setCompletedAt(LocalDateTime.now());
        currentHistory.setPerformedBy(performer);
        currentHistory.setNote(note != null ? note : (isSkipped ? "Bỏ qua công đoạn" : "Hoàn thành công đoạn"));
        historyRepo.save(currentHistory);

        // 3. Tìm công đoạn kế tiếp theo sequenceOrder
        Stage nextStage = stageRepo.findBySequenceOrder(currentStage.getSequenceOrder() + 1)
                .orElse(null);

        if (nextStage == null) {
            // Hết pipeline -> batch hoàn thành toàn bộ
            batch.setStatus(BatchStatus.COMPLETED);
            batch.setCompletedAt(LocalDateTime.now());
            log.info("Mẻ gốm #{} đã hoàn tất toàn bộ quy trình sản xuất!", batch.getBatchCode());
        } else {
            batch.setCurrentStage(nextStage);
            batch.setStatus(BatchStatus.IN_PROGRESS);

            BatchStageHistory nextHistory = historyRepo.findByBatchIdAndStageId(batchId, nextStage.getId())
                    .orElseGet(() -> {
                        BatchStageHistory h = new BatchStageHistory();
                        h.setBatch(batch);
                        h.setStage(nextStage);
                        return h;
                    });

            nextHistory.setStatus(HistoryStatus.IN_PROGRESS);
            nextHistory.setStartedAt(LocalDateTime.now());
            historyRepo.save(nextHistory);
        }

        Batch savedBatch = batchRepo.save(batch);

        // 4. Bắn thông báo async (không block request chính)
        notificationService.sendStageCompletedAlert(savedBatch, currentStage, nextStage);

        return mapToResponse(savedBatch);
    }

    @Override
    public BatchResponse getBatchById(Long batchId) {
        Batch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
        return mapToResponse(batch);
    }

    @Override
    public List<BatchResponse> getAllBatches() {
        return batchRepo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BatchResponse mapToResponse(Batch batch) {
        BatchResponse response = modelMapper.map(batch, BatchResponse.class);
        if (batch.getOrder() != null) {
            response.setOrderId(batch.getOrder().getId());
            response.setOrderCode(batch.getOrder().getOrderCode());
            if (batch.getOrder().getAiExtractions() != null && !batch.getOrder().getAiExtractions().isEmpty()) {
                response.setProductName(batch.getOrder().getAiExtractions().iterator().next().getProductName());
            } else {
                response.setProductName(batch.getOrder().getCustomerName());
            }
        }
        if (batch.getCurrentStage() != null) {
            response.setCurrentStage(modelMapper.map(batch.getCurrentStage(), StageDto.class));
        }

        List<BatchStageHistory> histories = historyRepo.findByBatchIdOrderByStageSequenceOrderAsc(batch.getId());
        List<BatchStageHistoryDto> historyDtos = histories.stream().map(h -> {
            BatchStageHistoryDto dto = modelMapper.map(h, BatchStageHistoryDto.class);
            dto.setBatchId(batch.getId());
            dto.setStage(modelMapper.map(h.getStage(), StageDto.class));
            if (h.getPerformedBy() != null) {
                dto.setPerformedById(h.getPerformedBy().getId());
                dto.setPerformedByName(h.getPerformedBy().getFullName());
            }
            return dto;
        }).sorted(Comparator.comparing(dto -> dto.getStage().getSequenceOrder()))
          .collect(Collectors.toList());

        response.setStageHistories(historyDtos);
        return response;
    }
}
