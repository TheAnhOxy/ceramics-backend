package com.ceramic.controller;

import com.ceramic.dto.ApiResponse;
import com.ceramic.dto.BatchAdvanceRequest;
import com.ceramic.dto.BatchResponse;
import com.ceramic.service.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class BatchController {

    private final PipelineService pipelineService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getAllBatches() {
        log.info("Lấy danh sách tất cả mẻ gốm tại xưởng");
        List<BatchResponse> batches = pipelineService.getAllBatches();

        ApiResponse<List<BatchResponse>> response = ApiResponse.<List<BatchResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách mẻ gốm thành công")
                .data(batches)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BatchResponse>> getBatchById(@PathVariable Long id) {
        log.info("Lấy chi tiết mẻ gốm ID: {}", id);
        BatchResponse batch = pipelineService.getBatchById(id);

        ApiResponse<BatchResponse> response = ApiResponse.<BatchResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy chi tiết mẻ gốm thành công")
                .data(batch)
                .build();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/advance")
    public ResponseEntity<ApiResponse<BatchResponse>> advanceStage(
            @PathVariable Long id,
            @RequestBody(required = false) BatchAdvanceRequest request
    ) {
        Long performedBy = (request != null) ? request.getPerformedBy() : null;
        String note = (request != null) ? request.getNote() : "Chuyển công đoạn từ Web";
        Boolean forceSkip = (request != null) ? request.getForceSkip() : false;

        log.info("Chuyển công đoạn mẻ gốm ID: {}, người thực hiện: {}", id, performedBy);
        BatchResponse updatedBatch = pipelineService.advanceStage(id, performedBy, note, forceSkip);

        ApiResponse<BatchResponse> response = ApiResponse.<BatchResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Chuyển công đoạn sản xuất thành công")
                .data(updatedBatch)
                .build();
        return ResponseEntity.ok(response);
    }
}
