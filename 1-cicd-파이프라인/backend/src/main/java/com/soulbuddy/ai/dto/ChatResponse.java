package com.soulbuddy.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResponse {

    private String assistantMessage;

    /** ANXIOUS | SAD | CALM | HAPPY | NEUTRAL | ANGRY */
    private String emotionTag;

    /** LOW | MEDIUM | HIGH */
    private String riskLevel;

    private String summary;

    private String memoryHint;

    private String recommendedAction;
}
