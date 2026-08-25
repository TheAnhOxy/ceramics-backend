package com.ceramic.service.impl;

import com.ceramic.dto.AiExtractionResultDto;
import com.ceramic.dto.BatchResponse;
import com.ceramic.dto.OrderCreateRequest;
import com.ceramic.dto.OrderResponse;
import com.ceramic.entity.*;
import com.ceramic.enums.BatchStatus;
import com.ceramic.enums.HistoryStatus;
import com.ceramic.enums.OrderStatus;
import com.ceramic.enums.PriorityLevel;
import com.ceramic.exception.ResourceNotFoundException;
import com.ceramic.repository.*;
import com.ceramic.service.AiExtractionService;
import com.ceramic.service.OrderService;
import com.ceramic.service.PipelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final AiExtractionRepository aiExtractionRepository;
    private final BatchRepository batchRepository;
    private final StageRepository stageRepository;
    private final BatchStageHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final AiExtractionService aiExtractionService;
    private final PipelineService pipelineService;
    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public OrderResponse createOrder(OrderCreateRequest request) {
        log.info("Tiếp nhận đơn hàng mới từ mô tả: {}", request.getRawDescription());

        // 1. Khởi tạo thông tin Order
        String orderCode = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        User creator = null;
        if (request.getCreatedBy() != null) {
            creator = userRepository.findById(request.getCreatedBy()).orElse(null);
        }

        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setCustomerName(request.getCustomerName() != null ? request.getCustomerName() : "Khách hàng xưởng gốm");
        order.setRawDescription(request.getRawDescription());
        order.setQuantity(request.getQuantity() != null ? request.getQuantity() : 100);
        order.setDeadlineDate(request.getDeadlineDate() != null ? request.getDeadlineDate() : LocalDate.now().plusDays(10));
        order.setStatus(OrderStatus.PROCESSING);
        order.setCreatedBy(creator);

        Order savedOrder = orderRepository.save(order);

        // 2. Gọi AI Extraction Service bóc tách thông số kỹ thuật (3 retries, validation)
        AiExtractionResultDto aiResult = aiExtractionService.extract(request.getRawDescription());

        // Cập nhật lại quantity/deadline nếu AI bóc tách chính xác hơn
        if (aiResult.getQuantity() != null && aiResult.getQuantity() > 0) {
            savedOrder.setQuantity(aiResult.getQuantity());
        }
        if (aiResult.getDeadlineDays() != null && aiResult.getDeadlineDays() > 0) {
            savedOrder.setDeadlineDate(LocalDate.now().plusDays(aiResult.getDeadlineDays()));
        }
        savedOrder = orderRepository.save(savedOrder);

        // Lưu bản ghi AI Extractions
        AiExtraction aiEntity = new AiExtraction();
        aiEntity.setOrder(savedOrder);
        aiEntity.setProductName(aiResult.getProductName());
        aiEntity.setPattern(aiResult.getPattern());
        aiEntity.setHeightCm(aiResult.getHeightCm());
        aiEntity.setGlazeType(aiResult.getGlazeType());
        aiEntity.setEstimatedClayKg(aiResult.getEstimatedClayKg());
        aiEntity.setFiringTempCelsius(aiResult.getFiringTempCelsius());
        aiEntity.setFiringDurationHours(aiResult.getFiringDurationHours());
        
        PriorityLevel priority = PriorityLevel.NORMAL;
        if (aiResult.getPriorityLevel() != null) {
            try {
                priority = PriorityLevel.valueOf(aiResult.getPriorityLevel().toUpperCase());
            } catch (Exception ignored) {}
        }
        aiEntity.setPriorityLevel(priority);
        try {
            aiEntity.setRawAiJson(objectMapper.writeValueAsString(aiResult));
        } catch (Exception e) {
            aiEntity.setRawAiJson("{}");
        }
        aiEntity.setAiModel("OpenAI/Claude API");
        aiEntity.setConfidenceNote(aiResult.getConfidenceNote());
        aiExtractionRepository.save(aiEntity);

        // 3. Tự động tạo Batch + khởi tạo 6 bản ghi stage history
        List<Stage> stages = stageRepository.findAllByOrderBySequenceOrderAsc();
        if (stages.isEmpty()) {
            throw new ResourceNotFoundException("Chưa cấu hình các công đoạn sản xuất trong cơ sở dữ liệu");
        }

        Stage firstStage = stages.get(0);
        String batchCode = "GOM-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        Batch batch = new Batch();
        batch.setBatchCode(batchCode);
        batch.setOrder(savedOrder);
        batch.setQuantity(savedOrder.getQuantity());
        batch.setCurrentStage(firstStage);
        batch.setStatus(BatchStatus.IN_PROGRESS);
        batch.setPriorityLevel(priority);
        batch.setStartedAt(LocalDateTime.now());

        Batch savedBatch = batchRepository.save(batch);

        // Khởi tạo 6 bản ghi stage history (Stage 1 status = IN_PROGRESS, Stage 2-6 status = PENDING)
        for (int i = 0; i < stages.size(); i++) {
            Stage stage = stages.get(i);
            BatchStageHistory history = new BatchStageHistory();
            history.setBatch(savedBatch);
            history.setStage(stage);
            if (i == 0) {
                history.setStatus(HistoryStatus.IN_PROGRESS);
                history.setStartedAt(LocalDateTime.now());
            } else {
                history.setStatus(HistoryStatus.PENDING);
            }
            historyRepository.save(history);
        }

        log.info("Đã tạo tự động Batch #{} với 6 công đoạn cho Order #{}", batchCode, orderCode);
        return getOrderById(savedOrder.getId());
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + id));

        OrderResponse response = modelMapper.map(order, OrderResponse.class);
        if (order.getCreatedBy() != null) {
            response.setCreatedById(order.getCreatedBy().getId());
            response.setCreatedByName(order.getCreatedBy().getFullName());
        }

        // Map AI Extractions
        aiExtractionRepository.findByOrderId(order.getId()).ifPresent(ai -> {
            AiExtractionResultDto dto = new AiExtractionResultDto();
            dto.setProductName(ai.getProductName());
            dto.setPattern(ai.getPattern());
            dto.setHeightCm(ai.getHeightCm());
            dto.setGlazeType(ai.getGlazeType());
            dto.setEstimatedClayKg(ai.getEstimatedClayKg());
            dto.setFiringTempCelsius(ai.getFiringTempCelsius());
            dto.setFiringDurationHours(ai.getFiringDurationHours());
            dto.setPriorityLevel(ai.getPriorityLevel() != null ? ai.getPriorityLevel().name() : "NORMAL");
            dto.setConfidenceNote(ai.getConfidenceNote());
            response.setAiExtraction(dto);
        });

        // Map Batches
        List<Batch> orderBatches = batchRepository.findByOrderId(order.getId());
        if (orderBatches != null && !orderBatches.isEmpty()) {
            List<BatchResponse> batches = orderBatches.stream()
                    .map(b -> pipelineService.getBatchById(b.getId()))
                    .collect(Collectors.toList());
            response.setBatches(batches);
        }

        return response;
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> getOrderById(order.getId()))
                .collect(Collectors.toList());
    }
}
