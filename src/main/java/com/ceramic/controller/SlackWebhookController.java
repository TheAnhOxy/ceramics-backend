package com.ceramic.controller;

import com.ceramic.service.PipelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/slack")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class SlackWebhookController {

    private final PipelineService pipelineService;
    private final ObjectMapper objectMapper;

    // 1. Xử lý Form URL-encoded từ Slack Interactive Button Callbacks
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> handleSlackFormWebhook(
            @RequestParam(value = "payload", required = false) String payloadFormParam,
            HttpServletRequest request
    ) {
        log.info("Nhận Slack Webhook Form UrlEncoded. Payload param: {}", payloadFormParam);
        if (payloadFormParam == null) {
            payloadFormParam = request.getParameter("payload");
        }
        return processSlackPayload(payloadFormParam, null);
    }

    // 2. Xử lý JSON Raw Body
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleSlackJsonWebhook(
            @RequestBody(required = false) Map<String, Object> jsonBody
    ) {
        log.info("Nhận Slack Webhook JSON Body: {}", jsonBody);
        return processSlackPayload(null, jsonBody);
    }

    // 3. Fallback cho các trường loại Content-Type khác
    @PostMapping(value = "/webhook")
    public ResponseEntity<Map<String, Object>> handleSlackFallbackWebhook(
            @RequestParam(value = "payload", required = false) String payloadFormParam,
            HttpServletRequest request
    ) {
        log.info("Nhận Slack Webhook Fallback");
        if (payloadFormParam == null) {
            payloadFormParam = request.getParameter("payload");
        }
        return processSlackPayload(payloadFormParam, null);
    }

    private ResponseEntity<Map<String, Object>> processSlackPayload(String payloadFormParam, Map<String, Object> jsonBody) {
        String callbackData = null;

        try {
            if (payloadFormParam != null && !payloadFormParam.isBlank()) {
                Map<?, ?> slackMap = objectMapper.readValue(payloadFormParam, Map.class);
                callbackData = extractCallbackDataFromMap(slackMap);
            } else if (jsonBody != null && !jsonBody.isEmpty()) {
                callbackData = extractCallbackDataFromMap(jsonBody);
            }

            if (callbackData != null && callbackData.startsWith("confirm_stage:")) {
                Long batchId = Long.parseLong(callbackData.split(":")[1]);
                log.info("Xử lý xác nhận chuyển công đoạn cho Batch ID: {} từ Slack Bot", batchId);

                pipelineService.advanceStage(batchId, null, "Xác nhận chuyển bước từ Slack Bot", false);

                // Trả về định dạng Slack Message chuẩn để Slack xóa biểu tượng ⚠️ và xác nhận ngay lập tức!
                return ResponseEntity.ok(Map.of(
                        "response_type", "ephemeral",
                        "text", "✅ *XÁC NHẬN THÀNH CÔNG:* Mẻ gốm ID #" + batchId + " đã được chuyển sang công đoạn tiếp theo!"
                ));
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý callback từ Slack: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok(Map.of(
                "response_type", "ephemeral",
                "text", "✅ Đã ghi nhận thông tin tương tác từ Slack!"
        ));
    }

    private String extractCallbackDataFromMap(Map<?, ?> map) {
        if (map == null) return null;

        if (map.containsKey("value")) {
            return String.valueOf(map.get("value"));
        }
        if (map.containsKey("callback_data")) {
            return String.valueOf(map.get("callback_data"));
        }

        if (map.containsKey("actions")) {
            List<?> actions = (List<?>) map.get("actions");
            if (actions != null && !actions.isEmpty()) {
                Map<?, ?> action = (Map<?, ?>) actions.get(0);
                if (action.containsKey("value")) {
                    return String.valueOf(action.get("value"));
                }
            }
        }
        return null;
    }
}
