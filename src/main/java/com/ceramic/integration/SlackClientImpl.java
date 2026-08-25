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
public class SlackClientImpl implements SlackClient {

    @Value("${slack.webhook-url:}")
    private String webhookUrl;

    @Value("${slack.channel:#xưởng-gốm-tiến-độ}")
    private String defaultChannel;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean sendMessage(String text, String callbackData, String buttonText) {
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.contains("YOUR/SLACK/WEBHOOK")) {
            log.info("📢 [SLACK/ZALO SIMULATOR] Gửi tới Slack Channel [{}]:\n{}\n[Nút bấm Slack/Zalo: {} -> {}]",
                    defaultChannel, text, buttonText != null ? buttonText : "Xác nhận", callbackData);
            return true;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("channel", defaultChannel);
            body.put("text", text);

            if (callbackData != null && !callbackData.isBlank()) {
                List<Map<String, Object>> blocks = new ArrayList<>();
                blocks.add(Map.of(
                        "type", "section",
                        "text", Map.of("type", "mrkdwn", "text", text)
                ));

                Map<String, Object> buttonElement = Map.of(
                        "type", "button",
                        "text", Map.of("type", "plain_text", "text", buttonText != null ? buttonText : "Xác nhận"),
                        "value", callbackData,
                        "action_id", "confirm_stage_action"
                );

                blocks.add(Map.of(
                        "type", "actions",
                        "elements", List.of(buttonElement)
                ));
                body.put("blocks", blocks);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Gửi thông báo Slack thành công tới channel: {}", defaultChannel);
                return true;
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo Slack: {}", e.getMessage());
        }
        return false;
    }
}
