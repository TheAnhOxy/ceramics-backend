package com.ceramic.dto;

import com.ceramic.enums.HistoryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchStageHistoryDto {
    private Long id;
    private Long batchId;
    private StageDto stage;
    private HistoryStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long performedById;
    private String performedByName;
    private String note;
    private LocalDateTime createdAt;
}
