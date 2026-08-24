package com.ceramic.service.impl;

import com.ceramic.dto.BatchResponse;
import com.ceramic.dto.DashboardStatsResponse;
import com.ceramic.dto.KanbanBoardResponse;
import com.ceramic.entity.Batch;
import com.ceramic.entity.QcRecord;
import com.ceramic.entity.Stage;
import com.ceramic.enums.AlertType;
import com.ceramic.enums.BatchStatus;
import com.ceramic.repository.*;
import com.ceramic.service.DashboardService;
import com.ceramic.service.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final BatchRepository batchRepository;
    private final StageRepository stageRepository;
    private final AlertRepository alertRepository;
    private final QcRecordRepository qcRecordRepository;
    private final PipelineService pipelineService;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        long totalOrders = orderRepository.count();
        List<Batch> batches = batchRepository.findAll();
        
        long totalBatches = batches.size();
        long activeBatches = batches.stream().filter(b -> b.getStatus() == BatchStatus.IN_PROGRESS).count();
        long completedBatches = batches.stream().filter(b -> b.getStatus() == BatchStatus.COMPLETED).count();
        long onHoldBatches = batches.stream().filter(b -> b.getStatus() == BatchStatus.ON_HOLD || b.getStatus() == BatchStatus.FAILED).count();

        long criticalAlerts = alertRepository.countByAlertType(AlertType.CRITICAL);

        // Quality pass rate calculation
        List<QcRecord> qcRecords = qcRecordRepository.findAll();
        double overallPassRate = 100.0;
        if (!qcRecords.isEmpty()) {
            long totalCheckedSum = qcRecords.stream().mapToLong(QcRecord::getTotalChecked).sum();
            long totalPassedSum = qcRecords.stream().mapToLong(QcRecord::getPassedCount).sum();
            if (totalCheckedSum > 0) {
                overallPassRate = Math.round(((double) totalPassedSum / totalCheckedSum * 100.0) * 100.0) / 100.0;
            }
        }

        // Batches per stage count
        Map<String, Long> batchesPerStage = new LinkedHashMap<>();
        List<Stage> stages = stageRepository.findAllByOrderBySequenceOrderAsc();
        for (Stage stage : stages) {
            long count = batches.stream()
                    .filter(b -> b.getCurrentStage() != null && b.getCurrentStage().getId().equals(stage.getId()) && b.getStatus() == BatchStatus.IN_PROGRESS)
                    .count();
            batchesPerStage.put(stage.getName(), count);
        }

        return DashboardStatsResponse.builder()
                .totalOrders(totalOrders)
                .totalBatches(totalBatches)
                .activeBatches(activeBatches)
                .completedBatches(completedBatches)
                .onHoldBatches(onHoldBatches)
                .criticalAlerts(criticalAlerts)
                .overallPassRatePercent(overallPassRate)
                .batchesPerStage(batchesPerStage)
                .build();
    }

    @Override
    public KanbanBoardResponse getKanbanBoard() {
        List<Stage> stages = stageRepository.findAllByOrderBySequenceOrderAsc();
        List<KanbanBoardResponse.KanbanColumn> columns = new ArrayList<>();

        for (Stage stage : stages) {
            List<Batch> stageBatches = batchRepository.findByCurrentStageId(stage.getId()).stream()
                    .filter(b -> b.getStatus() == BatchStatus.IN_PROGRESS || b.getStatus() == BatchStatus.PENDING)
                    .collect(Collectors.toList());

            List<BatchResponse> batchDtos = stageBatches.stream()
                    .map(b -> pipelineService.getBatchById(b.getId()))
                    .collect(Collectors.toList());

            columns.add(KanbanBoardResponse.KanbanColumn.builder()
                    .stageCode(stage.getCode())
                    .stageName(stage.getName())
                    .sequenceOrder(stage.getSequenceOrder())
                    .batches(batchDtos)
                    .build());
        }

        // Add COMPLETED column
        List<Batch> completedBatches = batchRepository.findByStatus(BatchStatus.COMPLETED);
        List<BatchResponse> completedBatchDtos = completedBatches.stream()
                .map(b -> pipelineService.getBatchById(b.getId()))
                .collect(Collectors.toList());

        columns.add(KanbanBoardResponse.KanbanColumn.builder()
                .stageCode("COMPLETED")
                .stageName("Hoàn Thành / Xuất Xưởng")
                .sequenceOrder(99)
                .batches(completedBatchDtos)
                .build());

        return KanbanBoardResponse.builder()
                .columns(columns)
                .build();
    }
}
