package com.soulbuddy.domain.chat.dto.response;

import com.soulbuddy.global.enums.PersonaType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionCreateResponse {

    private String sessionId;
    private PersonaType personaType;
    private String openingMessage;
    private String recentSummary;
    private LocalDateTime createdAt;
}
