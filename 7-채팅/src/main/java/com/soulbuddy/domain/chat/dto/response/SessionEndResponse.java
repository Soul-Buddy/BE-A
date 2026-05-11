package com.soulbuddy.domain.chat.dto.response;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.SessionStatus;
import com.soulbuddy.global.enums.SummaryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionEndResponse {

    private String sessionId;
    private SessionStatus status;
    private SummaryStatus summaryStatus;
    private String summaryText;
    private String situationText;
    private String emotionText;
    private String thoughtText;
    private EmotionTag dominantEmotion;
    private String emotionChange;
    private String memoryHint;
    private LocalDateTime endedAt;
}
