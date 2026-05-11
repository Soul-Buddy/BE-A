package com.soulbuddy.domain.chat.dto.response;

import com.soulbuddy.global.enums.PersonaType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PersonaResponse {

    private PersonaType personaType;
    private String characterName;
    private String displayName;
    private String description;
    private List<String> tags;
    private String toneSample;

    public static PersonaResponse from(PersonaType type) {
        if (type == PersonaType.FRIEND) {
            return PersonaResponse.builder()
                    .personaType(type)
                    .characterName(type.characterName())
                    .displayName(type.displayName())
                    .description("공감형·따뜻함")
                    .tags(List.of("#따뜻", "#공감"))
                    .toneSample("야 그거 진짜 힘들었겠다~")
                    .build();
        }
        return PersonaResponse.builder()
                .personaType(type)
                .characterName(type.characterName())
                .displayName(type.displayName())
                .description("성찰형·차분함")
                .tags(List.of("#차분", "#성찰"))
                .toneSample("그 상황에서 어떤 감정을 느끼셨나요?")
                .build();
    }
}
