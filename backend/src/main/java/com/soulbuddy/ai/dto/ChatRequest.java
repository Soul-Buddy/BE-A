package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.PersonaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
