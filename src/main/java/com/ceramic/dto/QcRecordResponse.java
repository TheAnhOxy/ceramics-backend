package com.ceramic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QcRecordResponse {
    private Long id;
    private Long batchId;
    private String batchCode;
    private Integer totalChecked;
    private Integer passedCount;
    private Integer failedCount;
    private Double defectRatePercent;
    private String defectType;
    private String defectNote;
    private Boolean isCritical;
    private Long checkedById;
    private String checkedByName;
    private LocalDateTime checkedAt;
}
