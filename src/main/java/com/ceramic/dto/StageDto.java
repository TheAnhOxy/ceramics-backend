package com.ceramic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageDto {
    private Integer id;
    private String code;
    private String name;
    private Integer sequenceOrder;
    private Integer defaultDurationHours;
}
