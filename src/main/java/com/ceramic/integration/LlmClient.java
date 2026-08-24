package com.ceramic.integration;

public interface LlmClient {
    String complete(String systemPrompt, String userPrompt);
}
