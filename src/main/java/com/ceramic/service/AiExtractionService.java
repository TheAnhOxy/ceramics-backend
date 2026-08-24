package com.ceramic.service;

import com.ceramic.dto.AiExtractionResultDto;

public interface AiExtractionService {
    AiExtractionResultDto extract(String rawDescription);
}
