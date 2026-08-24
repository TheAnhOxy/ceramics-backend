package com.ceramic.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class LlmClientImpl implements LlmClient {

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.api-url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (openAiApiKey != null && !openAiApiKey.isBlank() && !openAiApiKey.equals("YOUR_OPENAI_API_KEY")) {
            try {
                return callOpenAiApi(systemPrompt, userPrompt);
            } catch (Exception e) {
                log.warn("Lỗi khi gọi OpenAI API, chuyển sang Smart Rule Fallback Parser: {}", e.getMessage());
            }
        }
        log.info("Chạy Smart Rule Fallback Parser để bóc tách thông số đơn hàng...");
        return fallbackSmartParse(userPrompt);
    }

    private String callOpenAiApi(String systemPrompt, String userPrompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openAiModel);
        requestBody.put("response_format", Map.of("type", "json_object"));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(openAiApiUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<?, ?> jsonMap = objectMapper.readValue(response.getBody(), Map.class);
            List<?> choices = (List<?>) jsonMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                if (message != null && message.get("content") != null) {
                    return message.get("content").toString().trim();
                }
            }
        }
        throw new IllegalStateException("Phản hồi từ OpenAI API không hợp lệ");
    }

    private String fallbackSmartParse(String text) {
        String lower = text.toLowerCase();
        
        // Quantity extraction
        int quantity = 100;
        Matcher qtyMatcher = Pattern.compile("(\\d+)\\s*(cái|chiếc|bình|sản phẩm|đơn|bộ|mẻ)?").matcher(text);
        if (qtyMatcher.find()) {
            try {
                quantity = Integer.parseInt(qtyMatcher.group(1));
            } catch (Exception ignored) {}
        }

        // Height extraction
        Double height = 30.0;
        Matcher heightMatcher = Pattern.compile("cao\\s*(\\d+(?:\\.\\d+)?)\\s*cm").matcher(lower);
        if (heightMatcher.find()) {
            try {
                height = Double.parseDouble(heightMatcher.group(1));
            } catch (Exception ignored) {}
        }

        // Firing temp extraction
        int firingTemp = 1250;
        Matcher tempMatcher = Pattern.compile("(\\d{3,4})\\s*(?:°|o)?\\s*c").matcher(lower);
        if (tempMatcher.find()) {
            try {
                firingTemp = Integer.parseInt(tempMatcher.group(1));
            } catch (Exception ignored) {}
        }

        // Deadline extraction
        int deadlineDays = 7;
        Matcher deadlineMatcher = Pattern.compile("(\\d+)\\s*ngày").matcher(lower);
        if (deadlineMatcher.find()) {
            try {
                deadlineDays = Integer.parseInt(deadlineMatcher.group(1));
            } catch (Exception ignored) {}
        }

        // Glaze type extraction
        String glazeType = "Men lam truyền thống";
        if (lower.contains("men rạn")) glazeType = "Men rạn cổ";
        else if (lower.contains("men hỏa biến")) glazeType = "Men hỏa biến";
        else if (lower.contains("men ngọc")) glazeType = "Men ngọc bích";
        else if (lower.contains("men trắng")) glazeType = "Men trắng bóng";

        // Product name extraction
        String productName = "Sản phẩm gốm sứ thủ công";
        if (lower.contains("bình")) productName = "Bình gốm sứ";
        else if (lower.contains("lọ")) productName = "Lọ hoa gốm sứ";
        else if (lower.contains("chén") || lower.contains("tách")) productName = "Bộ chén trà gốm";
        else if (lower.contains("bát") || lower.contains("tô")) productName = "Bộ bát đĩa gốm";

        // Pattern extraction
        String pattern = "Họa tiết thủ công";
        if (lower.contains("sen")) pattern = "Họa tiết hoa sen";
        else if (lower.contains("rồng")) pattern = "Họa tiết rồng chầu";
        else if (lower.contains("tùng cúc trúc mai")) pattern = "Họa tiết Tùng Cúc Trúc Mai";

        // Priority determination
        String priority = (deadlineDays <= 7) ? "HIGH" : "NORMAL";
        if (deadlineDays <= 3) priority = "URGENT";

        // Clay estimation (approx 1.5kg per product)
        double estimatedClay = Math.round(quantity * 1.5 * 100.0) / 100.0;

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("product_name", productName);
            result.put("pattern", pattern);
            result.put("height_cm", height);
            result.put("diameter_cm", Math.round(height * 0.6 * 10.0) / 10.0);
            result.put("glaze_type", glazeType);
            result.put("quantity", quantity);
            result.put("estimated_clay_kg", estimatedClay);
            result.put("firing_temp_celsius", firingTemp);
            result.put("firing_duration_hours", 24);
            result.put("priority_level", priority);
            result.put("deadline_days", deadlineDays);
            result.put("confidence_note", "Bóc tách tự động bởi Smart Fallback AI Engine");

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"product_name\":\"Sản phẩm gốm\",\"quantity\":100,\"firing_temp_celsius\":1250,\"estimated_clay_kg\":150.0,\"priority_level\":\"NORMAL\"}";
        }
    }
}
