package com.soulbuddy.domain.chat.dto.response;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.enums.SessionStatus;
import com.soulbuddy.global.enums.SummaryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// GET /api/sessions 목록 내 세션 카드 단건
@Getter
@Builder
public class SessionItemResponse {

    private String sessionId;
    private PersonaType personaType;
    private String characterName;
    private SessionStatus status;
    private EmotionTag preChatEmotion;
    private SummaryStatus summaryStatus;
    private String quoteText;
    private EmotionTag dominantEmotion;
    private String emotionChange;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
