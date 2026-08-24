package com.ceramic.dto;

import com.ceramic.enums.BatchStatus;
import com.ceramic.enums.PriorityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {
    private Long id;
    private String batchCode;
    private Long orderId;
    private String orderCode;
    private String productName;
    private Integer quantity;
    private StageDto currentStage;
    private BatchStatus status;
    private PriorityLevel priorityLevel;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private List<BatchStageHistoryDto> stageHistories;
}
