package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.PersonaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChatRequest {

    @NotBlank
    private String sessionId;

    @NotNull
    private PersonaType personaType;

    @NotBlank
    @Size(max = 1000)
    private String message;

    private String recentSummary;
}
