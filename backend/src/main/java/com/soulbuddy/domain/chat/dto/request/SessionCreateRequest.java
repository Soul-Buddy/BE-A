package com.soulbuddy.domain.chat.dto.request;

import com.soulbuddy.global.enums.PersonaType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SessionCreateRequest {

    @NotNull
    private PersonaType personaType;
}
