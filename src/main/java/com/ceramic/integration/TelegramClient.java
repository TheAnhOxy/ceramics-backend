package com.ceramic.integration;

public interface TelegramClient {
    boolean sendMessage(String chatId, String text, String inlineCallbackData, String buttonText);
}
