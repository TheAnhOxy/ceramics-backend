package com.ceramic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchAdvanceRequest {
    private Long performedBy;
    private String note;
    private Boolean forceSkip = false;
}
