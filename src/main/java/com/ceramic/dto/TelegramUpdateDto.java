package com.ceramic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramUpdateDto {

    @JsonProperty("update_id")
    private Long updateId;

    @JsonProperty("callback_query")
    private CallbackQuery callbackQuery;

    @JsonProperty("message")
    private TelegramMessage message;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallbackQuery {
        private String id;
        private TelegramUser from;
        private TelegramMessage message;
        private String data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelegramMessage {
        @JsonProperty("message_id")
        private Long messageId;

        private TelegramUser from;

        private TelegramChat chat;

        private String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelegramChat {
        private Long id;
        private String title;
        private String type;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelegramUser {
        private Long id;
        
        @JsonProperty("is_bot")
        private Boolean isBot;

        @JsonProperty("first_name")
        private String firstName;

        private String username;
    }
}
