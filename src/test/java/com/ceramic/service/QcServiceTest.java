package com.ceramic.service;

import com.ceramic.dto.QcRecordRequest;
import com.ceramic.dto.QcRecordResponse;
import com.ceramic.entity.Batch;
import com.ceramic.entity.QcRecord;
import com.ceramic.repository.BatchRepository;
import com.ceramic.repository.QcRecordRepository;
import com.ceramic.repository.UserRepository;
import com.ceramic.service.impl.QcServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QcServiceTest {

    @Mock
    private QcRecordRepository qcRecordRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private QcService qcService;

    @BeforeEach
    void setUp() {
        ModelMapper modelMapper = new ModelMapper();
        qcService = new QcServiceImpl(qcRecordRepository, batchRepository, userRepository, notificationService, modelMapper);
    }

    @Test
    @DisplayName("Ghi nhận QC với tỉ lệ lỗi > 3% phải bắn Cảnh báo đỏ (Critical Alert)")
    void testRecordQcWithCriticalDefectRate() {
        Batch batch = new Batch();
        batch.setId(1L);
        batch.setBatchCode("GOM-001");

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(qcRecordRepository.save(any(QcRecord.class))).thenAnswer(i -> {
            QcRecord r = i.getArgument(0);
            r.setId(10L);
            return r;
        });

        QcRecordRequest request = new QcRecordRequest();
        request.setBatchId(1L);
        request.setTotalChecked(100);
        request.setPassedCount(90);
        request.setFailedCount(10); // 10% > 3% threshold
        request.setDefectType("Nứt men");

        QcRecordResponse response = qcService.recordQc(request);

        assertNotNull(response);
        assertTrue(response.getIsCritical());
        assertEquals(10.0, response.getDefectRatePercent());
        verify(notificationService, times(1)).sendCriticalAlert(eq(1L), anyString());
    }

    @Test
    @DisplayName("Ghi nhận QC bình thường với tỉ lệ lỗi <= 3% không bắn Cảnh báo đỏ")
    void testRecordQcNormalDefectRate() {
        Batch batch = new Batch();
        batch.setId(1L);
        batch.setBatchCode("GOM-001");

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(qcRecordRepository.save(any(QcRecord.class))).thenAnswer(i -> {
            QcRecord r = i.getArgument(0);
            r.setId(11L);
            return r;
        });

        QcRecordRequest request = new QcRecordRequest();
        request.setBatchId(1L);
        request.setTotalChecked(100);
        request.setPassedCount(98);
        request.setFailedCount(2); // 2% <= 3% threshold
        request.setDefectType("Vết bẩn men nhỏ");

        QcRecordResponse response = qcService.recordQc(request);

        assertNotNull(response);
        assertFalse(response.getIsCritical());
        assertEquals(2.0, response.getDefectRatePercent());
        verify(notificationService, never()).sendCriticalAlert(anyLong(), anyString());
    }
}
