package com.soulbuddy.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatHistoryResponse {

    private String sessionId;

    private List<MessageItem> messages;

    private long totalCount;

    private int page;

    private int size;

    @Getter
    @Builder
    public static class MessageItem {
        private Long messageId;
        private String sender;
        private String content;
        private String emotionTag;
        private LocalDateTime createdAt;
    }
}
