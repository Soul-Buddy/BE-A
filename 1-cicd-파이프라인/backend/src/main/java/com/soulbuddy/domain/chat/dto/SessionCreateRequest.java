package com.soulbuddy.domain.chat.dto;

import com.soulbuddy.global.enums.PersonaType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SessionCreateRequest {

    @NotNull
    private PersonaType personaType;
}
