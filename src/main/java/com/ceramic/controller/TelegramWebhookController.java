package com.ceramic.controller;

import com.ceramic.dto.ApiResponse;
import com.ceramic.dto.TelegramUpdateDto;
import com.ceramic.service.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telegram")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final PipelineService pipelineService;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> handleWebhook(@RequestBody TelegramUpdateDto update) {
        log.info("Nhận Telegram Update Webhook: {}", update);

        if (update != null && update.getCallbackQuery() != null) {
            String callbackData = update.getCallbackQuery().getData(); // "confirm_stage:12"
            log.info("Nhận callback data từ nút bấm Telegram: {}", callbackData);

            if (callbackData != null && callbackData.startsWith("confirm_stage:")) {
                try {
                    Long batchId = Long.parseLong(callbackData.split(":")[1]);
                    String username = (update.getCallbackQuery().getFrom() != null)
                            ? update.getCallbackQuery().getFrom().getUsername()
                            : "Telegram User";

                    pipelineService.advanceStage(batchId, null, "Xác nhận qua Telegram Bot bởi @" + username, false);

                    ApiResponse<String> response = ApiResponse.<String>builder()
                            .status(HttpStatus.OK.value())
                            .message("Xác nhận chuyển công đoạn từ Telegram thành công cho Mẻ ID: " + batchId)
                            .data("OK")
                            .build();
                    return ResponseEntity.ok(response);
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý callback Telegram: {}", e.getMessage());
                }
            }
        }

        ApiResponse<String> response = ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Đã nhận tin nhắn Telegram Webhook")
                .data("IGNORED")
                .build();
        return ResponseEntity.ok(response);
    }
}
