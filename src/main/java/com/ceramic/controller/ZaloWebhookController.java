package com.ceramic.controller;

import com.ceramic.dto.ApiResponse;
import com.ceramic.service.PipelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/zalo")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class ZaloWebhookController {

    private final PipelineService pipelineService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<ApiResponse<String>> handleZaloFormWebhook(
            @RequestParam(value = "data", required = false) String dataParam,
            HttpServletRequest request
    ) {
        log.info("Nhận Zalo OA Webhook Form UrlEncoded");
        if (dataParam == null) {
            dataParam = request.getParameter("data");
        }
        return processZaloPayload(dataParam, null);
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> handleZaloJsonWebhook(
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        log.info("Nhận Zalo OA Webhook JSON Body: {}", payload);
        return processZaloPayload(null, payload);
    }

    @PostMapping(value = "/webhook")
    public ResponseEntity<ApiResponse<String>> handleZaloFallbackWebhook(
            @RequestParam(value = "data", required = false) String dataParam,
            HttpServletRequest request
    ) {
        log.info("Nhận Zalo OA Webhook Fallback");
        if (dataParam == null) {
            dataParam = request.getParameter("data");
        }
        return processZaloPayload(dataParam, null);
    }

    private ResponseEntity<ApiResponse<String>> processZaloPayload(String dataParam, Map<String, Object> payload) {
        String callbackData = dataParam;

        if (payload != null) {
            if (payload.containsKey("data")) {
                callbackData = String.valueOf(payload.get("data"));
            } else if (payload.containsKey("event_name")) {
                callbackData = String.valueOf(payload.get("event_name"));
            }
        }

        if (callbackData != null && callbackData.startsWith("confirm_stage:")) {
            try {
                Long batchId = Long.parseLong(callbackData.split(":")[1]);
                pipelineService.advanceStage(batchId, null, "Xác nhận qua Zalo OA Bot", false);

                ApiResponse<String> response = ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message("Xác nhận chuyển công đoạn từ Zalo thành công cho Mẻ ID: " + batchId)
                        .data("OK")
                        .build();
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Lỗi khi xử lý callback Zalo: {}", e.getMessage());
            }
        }

        ApiResponse<String> response = ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Đã nhận tin nhắn Zalo Webhook")
                .data("SUCCESS")
                .build();
        return ResponseEntity.ok(response);
    }
}
