package com.ceramic.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ZaloClientImpl implements ZaloClient {

    @Value("${zalo.webhook-url:}")
    private String zaloWebhookUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean sendMessage(String text, String callbackData, String buttonText) {
        if (zaloWebhookUrl == null || zaloWebhookUrl.isBlank() || zaloWebhookUrl.contains("openapi.zalo.me")) {
            log.info("📢 [ZALO OA SIMULATOR] Gửi tin nhắn Zalo OA:\n{}\n[Nút bấm Zalo: {} -> {}]",
                    text, buttonText != null ? buttonText : "Xác nhận", callbackData);
            return true;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("recipient", Map.of("user_id", "DEFAULT_ZALO_USER"));
            body.put("message", Map.of("text", text));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(zaloWebhookUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Gửi thông báo Zalo OA thành công");
                return true;
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo Zalo: {}", e.getMessage());
        }
        return false;
    }
}
