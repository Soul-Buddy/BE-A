package com.soulbuddy.domain.chat.dto.response;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.InterventionType;
import com.soulbuddy.global.enums.RiskLevel;
import com.soulbuddy.global.enums.Sender;
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
        private Sender sender;
        private String content;
        private EmotionTag emotionTag;
        private RiskLevel riskLevel;
        private InterventionType interventionType;
        private boolean ragUsed;
        private String aiModel;
        private LocalDateTime createdAt;
    }
}
