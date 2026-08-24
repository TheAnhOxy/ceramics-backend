package com.ceramic.service.impl;

import com.ceramic.dto.QcRecordRequest;
import com.ceramic.dto.QcRecordResponse;
import com.ceramic.entity.Batch;
import com.ceramic.entity.QcRecord;
import com.ceramic.entity.User;
import com.ceramic.exception.BatchNotFoundException;
import com.ceramic.repository.BatchRepository;
import com.ceramic.repository.QcRecordRepository;
import com.ceramic.repository.UserRepository;
import com.ceramic.service.NotificationService;
import com.ceramic.service.QcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QcServiceImpl implements QcService {

    private final QcRecordRepository qcRecordRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    private static final double CRITICAL_DEFECT_THRESHOLD = 0.03; // Ngưỡng cảnh báo 3% lỗi

    @Transactional
    @Override
    public QcRecordResponse recordQc(QcRecordRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new BatchNotFoundException(request.getBatchId()));

        User inspector = null;
        if (request.getCheckedBy() != null) {
            inspector = userRepository.findById(request.getCheckedBy()).orElse(null);
        }

        double defectRate = (double) request.getFailedCount() / request.getTotalChecked();
        boolean isCritical = defectRate > CRITICAL_DEFECT_THRESHOLD;

        QcRecord qc = new QcRecord();
        qc.setBatch(batch);
        qc.setTotalChecked(request.getTotalChecked());
        qc.setPassedCount(request.getPassedCount());
        qc.setFailedCount(request.getFailedCount());
        qc.setDefectType(request.getDefectType() != null ? request.getDefectType() : "Nứt men / Khuyết tật mộc");
        qc.setDefectNote(request.getDefectNote());
        qc.setIsCritical(isCritical);
        qc.setCheckedBy(inspector);

        QcRecord savedQc = qcRecordRepository.save(qc);

        if (isCritical) {
            String alertMessage = String.format("Mẻ gốm #%s (ID: %d) có tỉ lệ lỗi %.2f%% (%d/%d bị %s). Vượt ngưỡng cho phép 3%%!",
                    batch.getBatchCode(),
                    batch.getId(),
                    defectRate * 100,
                    request.getFailedCount(),
                    request.getTotalChecked(),
                    qc.getDefectType());

            log.warn("🚨 [CRITICAL QC ALERT] {}", alertMessage);
            notificationService.sendCriticalAlert(batch.getId(), alertMessage);
        }

        return mapToResponse(savedQc);
    }

    @Override
    public List<QcRecordResponse> getQcRecordsByBatchId(Long batchId) {
        return qcRecordRepository.findByBatchId(batchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private QcRecordResponse mapToResponse(QcRecord qc) {
        QcRecordResponse response = modelMapper.map(qc, QcRecordResponse.class);
        if (qc.getBatch() != null) {
            response.setBatchId(qc.getBatch().getId());
            response.setBatchCode(qc.getBatch().getBatchCode());
        }
        if (qc.getCheckedBy() != null) {
            response.setCheckedById(qc.getCheckedBy().getId());
            response.setCheckedByName(qc.getCheckedBy().getFullName());
        }
        double rate = (qc.getTotalChecked() > 0) ? ((double) qc.getFailedCount() / qc.getTotalChecked()) * 100 : 0;
        response.setDefectRatePercent(Math.round(rate * 100.0) / 100.0);
        return response;
    }
}
