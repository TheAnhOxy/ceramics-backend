package com.ceramic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    @NotBlank(message = "Mô tả đơn hàng không được để trống")
    private String rawDescription;

    private String customerName;

    private Integer quantity;

    private LocalDate deadlineDate;

    private Long createdBy;
}
