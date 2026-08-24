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
public class TelegramClientImpl implements TelegramClient {

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.chat-id:}")
    private String defaultChatId;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean sendMessage(String chatId, String text, String inlineCallbackData, String buttonText) {
        String targetChatId = (chatId != null && !chatId.isBlank()) ? chatId : defaultChatId;

        if (botToken == null || botToken.isBlank() || botToken.equals("YOUR_TELEGRAM_BOT_TOKEN") || targetChatId == null || targetChatId.isBlank()) {
            log.info("📢 [TELEGRAM SIMULATOR] Gửi tới ChatId [{}]:\n{}\n[Inline Button: {} -> {}]",
                    targetChatId != null ? targetChatId : "LOG_ONLY", text, buttonText, inlineCallbackData);
            return true;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", targetChatId);
            body.put("text", text);
            body.put("parse_mode", "Markdown");

            if (inlineCallbackData != null && !inlineCallbackData.isBlank()) {
                Map<String, Object> inlineButton = new HashMap<>();
                inlineButton.put("text", buttonText != null ? buttonText : "Xác nhận");
                inlineButton.put("callback_data", inlineCallbackData);

                Map<String, Object> replyMarkup = Map.of(
                        "inline_keyboard", List.of(List.of(inlineButton))
                );
                body.put("reply_markup", replyMarkup);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Gửi thông báo Telegram thành công tới Chat ID: {}", targetChatId);
                return true;
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo Telegram: {}", e.getMessage());
        }
        return false;
    }
}
