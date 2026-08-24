package com.ceramic.service;

import com.ceramic.dto.BatchResponse;

import java.util.List;

public interface PipelineService {
    BatchResponse advanceStage(Long batchId, Long performedBy, String note, Boolean forceSkip);
    BatchResponse getBatchById(Long batchId);
    List<BatchResponse> getAllBatches();
}
