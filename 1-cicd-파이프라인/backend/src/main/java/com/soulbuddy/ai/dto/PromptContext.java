package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.enums.PreferredTone;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PromptContext {

    private String nickname;

    private PreferredTone preferredTone;

    private String personality;

    private List<String> hobbies;

    private List<String> concerns;

    private String recentSummary;

    private PersonaType personaType;
}
