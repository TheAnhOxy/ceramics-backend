package com.ceramic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanBoardResponse {
    private List<KanbanColumn> columns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KanbanColumn {
        private String stageCode;
        private String stageName;
        private Integer sequenceOrder;
        private List<BatchResponse> batches;
    }
}
