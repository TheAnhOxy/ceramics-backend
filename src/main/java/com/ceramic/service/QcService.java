package com.ceramic.service;

import com.ceramic.dto.QcRecordRequest;
import com.ceramic.dto.QcRecordResponse;

import java.util.List;

public interface QcService {
    QcRecordResponse recordQc(QcRecordRequest request);
    List<QcRecordResponse> getQcRecordsByBatchId(Long batchId);
}
