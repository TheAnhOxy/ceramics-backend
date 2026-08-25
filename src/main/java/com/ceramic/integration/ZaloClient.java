package com.ceramic.integration;

public interface ZaloClient {
    boolean sendMessage(String text, String callbackData, String buttonText);
}
