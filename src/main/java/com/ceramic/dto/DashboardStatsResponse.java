package com.ceramic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalOrders;
    private long totalBatches;
    private long activeBatches;
    private long completedBatches;
    private long onHoldBatches;
    private long criticalAlerts;
    private double overallPassRatePercent;
    private Map<String, Long> batchesPerStage;
}
