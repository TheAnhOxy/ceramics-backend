package com.ceramic.integration;

public interface SlackClient {
    boolean sendMessage(String text, String callbackData, String buttonText);
}
