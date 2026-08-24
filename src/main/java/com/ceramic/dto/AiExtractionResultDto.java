package com.ceramic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiExtractionResultDto {

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("pattern")
    private String pattern;

    @JsonProperty("height_cm")
    private BigDecimal heightCm;

    @JsonProperty("diameter_cm")
    private BigDecimal diameterCm;

    @JsonProperty("glaze_type")
    private String glazeType;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("estimated_clay_kg")
    private BigDecimal estimatedClayKg;

    @JsonProperty("firing_temp_celsius")
    private Integer firingTempCelsius;

    @JsonProperty("firing_duration_hours")
    private Integer firingDurationHours;

    @JsonProperty("priority_level")
    private String priorityLevel;

    @JsonProperty("deadline_days")
    private Integer deadlineDays;

    @JsonProperty("confidence_note")
    private String confidenceNote;
}
