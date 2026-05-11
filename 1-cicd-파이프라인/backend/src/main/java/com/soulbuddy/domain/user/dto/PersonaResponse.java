package com.soulbuddy.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PersonaResponse {

    private String personaType;

    private String displayName;

    private String description;

    private String toneSample;
}
