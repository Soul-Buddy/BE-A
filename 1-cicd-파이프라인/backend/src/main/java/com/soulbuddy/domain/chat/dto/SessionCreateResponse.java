package com.soulbuddy.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionCreateResponse {

    private String sessionId;

    private String personaType;

    private String recentSummary;

    private LocalDateTime createdAt;
}
