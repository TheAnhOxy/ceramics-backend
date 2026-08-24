package com.ceramic.service.impl;

import com.ceramic.dto.AiExtractionResultDto;
import com.ceramic.exception.AiExtractionFailedException;
import com.ceramic.integration.LlmClient;
import com.ceramic.service.AiExtractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiExtractionServiceImpl implements AiExtractionService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        Bạn là AI Agent chuyên bóc tách thông số kỹ thuật đơn hàng gốm sứ.
        CHỈ trả về JSON hợp lệ theo đúng schema sau, KHÔNG thêm text giải thích,
        KHÔNG dùng markdown code fence:
        {
          "product_name": string,
          "pattern": string,
          "height_cm": number | null,
          "diameter_cm": number | null,
          "glaze_type": string,
          "quantity": integer,
          "estimated_clay_kg": number,
          "firing_temp_celsius": integer,
          "firing_duration_hours": integer,
          "priority_level": "LOW" | "NORMAL" | "HIGH" | "URGENT",
          "deadline_days": integer,
          "confidence_note": string
        }
        Quy tắc ước tính:
        - estimated_clay_kg ≈ quantity * hệ số theo kích thước sản phẩm
        - Nếu deadline < 7 ngày → priority_level = URGENT hoặc HIGH
        - Nếu thiếu field bắt buộc, hãy suy luận hợp lý và ghi chú trong confidence_note
        """;

    @Override
    public AiExtractionResultDto extract(String rawDescription) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < 3) {
            attempts++;
            try {
                String raw = llmClient.complete(SYSTEM_PROMPT, rawDescription);
                log.info("Lần thử AI thứ {}: Chuỗi nhận được: {}", attempts, raw);

                // Clean potential markdown wrap if any
                String cleanedJson = cleanJsonString(raw);
                AiExtractionResultDto result = objectMapper.readValue(cleanedJson, AiExtractionResultDto.class);
                
                validate(result); // Throw exception if required fields are missing/invalid
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("AI trả JSON không hợp lệ ở lần thử {}: {}", attempts, e.getMessage());
            }
        }
        throw new AiExtractionFailedException("Không thể trích xuất thông số kỹ thuật sau 3 lần thử: " + (lastException != null ? lastException.getMessage() : "Lỗi không xác định"));
    }

    private String cleanJsonString(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private void validate(AiExtractionResultDto r) {
        if (r == null) {
            throw new IllegalArgumentException("Kết quả AI trích xuất bị null");
        }
        if (r.getProductName() == null || r.getProductName().isBlank()) {
            throw new IllegalArgumentException("Thiếu tên sản phẩm (product_name)");
        }
        if (r.getQuantity() == null || r.getQuantity() <= 0) {
            throw new IllegalArgumentException("Số lượng (quantity) phải lớn hơn 0");
        }
        if (r.getFiringTempCelsius() == null || r.getFiringTempCelsius() <= 0) {
            throw new IllegalArgumentException("Nhiệt độ nung (firing_temp_celsius) không hợp lệ");
        }
    }
}
