package com.ceramic.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QcRecordRequest {
    @NotNull(message = "Batch ID không được để trống")
    private Long batchId;

    @NotNull(message = "Tổng số lượng kiểm tra không được để trống")
    @Min(value = 1, message = "Tổng số lượng kiểm tra phải lớn hơn 0")
    private Integer totalChecked;

    @NotNull(message = "Số lượng đạt không được để trống")
    @Min(value = 0, message = "Số lượng đạt không thể âm")
    private Integer passedCount;

    @NotNull(message = "Số lượng lỗi không được để trống")
    @Min(value = 0, message = "Số lượng lỗi không thể âm")
    private Integer failedCount;

    private String defectType;
    private String defectNote;
    private Long checkedBy;
}
