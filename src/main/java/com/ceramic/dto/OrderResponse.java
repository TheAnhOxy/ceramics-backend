package com.ceramic.dto;

import com.ceramic.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderCode;
    private String customerName;
    private String rawDescription;
    private Integer quantity;
    private LocalDate deadlineDate;
    private OrderStatus status;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private AiExtractionResultDto aiExtraction;
    private List<BatchResponse> batches;
}
