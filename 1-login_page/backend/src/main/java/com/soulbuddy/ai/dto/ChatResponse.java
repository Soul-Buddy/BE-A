package com.soulbuddy.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResponse {

    private String assistantMessage;

    /** HAPPY | SAD | ANGRY | ANXIOUS | HURT | EMBARRASSED */
    private String emotionTag;

    /** LOW | MEDIUM | HIGH */
    private String riskLevel;

    private String summary;

    private String memoryHint;

    private String recommendedAction;
}
