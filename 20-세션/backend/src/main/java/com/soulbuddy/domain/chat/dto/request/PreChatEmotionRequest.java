package com.soulbuddy.domain.chat.dto.request;

import com.soulbuddy.global.enums.EmotionTag;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PreChatEmotionRequest {

    @NotNull
    private EmotionTag preChatEmotion;
}
