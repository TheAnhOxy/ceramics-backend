package com.ceramic.controller;

import com.ceramic.dto.ApiResponse;
import com.ceramic.dto.QcRecordRequest;
import com.ceramic.dto.QcRecordResponse;
import com.ceramic.service.QcService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qc")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class QcController {

    private final QcService qcService;

    @PostMapping
    public ResponseEntity<ApiResponse<QcRecordResponse>> recordQc(@Valid @RequestBody QcRecordRequest request) {
        log.info("Ghi nhận kết quả kiểm định QC cho mẻ gốm ID: {}, Số sản phẩm lỗi: {}", request.getBatchId(), request.getFailedCount());
        QcRecordResponse result = qcService.recordQc(request);

        String message = Boolean.TRUE.equals(result.getIsCritical())
                ? "Ghi nhận QC thành công - CẢNH BÁO ĐỎ! Tỉ lệ lỗi vượt ngưỡng cho phép 3%"
                : "Ghi nhận kết quả QC thành công";

        ApiResponse<QcRecordResponse> response = ApiResponse.<QcRecordResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(message)
                .data(result)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<ApiResponse<List<QcRecordResponse>>> getQcRecordsByBatchId(@PathVariable Long batchId) {
        log.info("Lấy lịch sử kiểm định QC cho mẻ gốm ID: {}", batchId);
        List<QcRecordResponse> records = qcService.getQcRecordsByBatchId(batchId);

        ApiResponse<List<QcRecordResponse>> response = ApiResponse.<List<QcRecordResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách bản ghi QC thành công")
                .data(records)
                .build();
        return ResponseEntity.ok(response);
    }
}
