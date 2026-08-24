package com.ceramic.service;

import com.ceramic.dto.AiExtractionResultDto;
import com.ceramic.exception.AiExtractionFailedException;
import com.ceramic.integration.LlmClient;
import com.ceramic.service.impl.AiExtractionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiExtractionServiceTest {

    @Mock
    private LlmClient llmClient;

    private AiExtractionService aiExtractionService;

    @BeforeEach
    void setUp() {
        aiExtractionService = new AiExtractionServiceImpl(llmClient, new ObjectMapper());
    }

    @Test
    @DisplayName("Bóc tách AI thành công ở lần thử đầu tiên")
    void testExtractSuccessFirstAttempt() {
        String validJson = """
            {
              "product_name": "Bình gốm họa tiết sen",
              "pattern": "Men lam",
              "height_cm": 35.0,
              "glaze_type": "Men lam truyền thống",
              "quantity": 200,
              "estimated_clay_kg": 300.0,
              "firing_temp_celsius": 1280,
              "firing_duration_hours": 24,
              "priority_level": "HIGH",
              "deadline_days": 10,
              "confidence_note": "Trích xuất chuẩn"
            }
            """;

        when(llmClient.complete(anyString(), anyString())).thenReturn(validJson);

        AiExtractionResultDto result = aiExtractionService.extract("Đơn 200 Bình gốm cao 35cm");

        assertNotNull(result);
        assertEquals("Bình gốm họa tiết sen", result.getProductName());
        assertEquals(200, result.getQuantity());
        assertEquals(1280, result.getFiringTempCelsius());
        verify(llmClient, times(1)).complete(anyString(), anyString());
    }

    @Test
    @DisplayName("Thử lại 3 lần khi JSON trả về không đúng schema và ném AiExtractionFailedException")
    void testExtractRetryThreeTimesAndFail() {
        String invalidJson = "{\"invalid_field\": true}";

        when(llmClient.complete(anyString(), anyString())).thenReturn(invalidJson);

        assertThrows(AiExtractionFailedException.class, () -> {
            aiExtractionService.extract("Đơn gốm thử nghiệm");
        });

        verify(llmClient, times(3)).complete(anyString(), anyString());
    }
}
