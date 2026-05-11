package com.soulbuddy.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionEndResponse {

    private String sessionId;

    private String summary;

    private String dominantEmotion;

    private String memoryHint;

    private LocalDateTime endedAt;
}
