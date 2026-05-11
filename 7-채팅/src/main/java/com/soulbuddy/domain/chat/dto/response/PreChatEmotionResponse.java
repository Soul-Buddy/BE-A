package com.soulbuddy.domain.chat.dto.response;

import com.soulbuddy.global.enums.EmotionTag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PreChatEmotionResponse {

    private String sessionId;
    private EmotionTag preChatEmotion;
}
