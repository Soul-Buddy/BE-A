package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.InterventionType;
import com.soulbuddy.global.enums.RiskLevel;
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
public class ChatResponse {

    private String assistantMessage;
    private EmotionTag emotionTag;
    private RiskLevel riskLevel;
    private InterventionType interventionType;
    private boolean ragUsed;
    private String aiModel;
    private boolean forcedSafety;
    private String summary;
    private String memoryHint;
    private String recommendedAction;
}
