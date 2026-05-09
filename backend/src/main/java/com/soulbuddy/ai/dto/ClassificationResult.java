package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.InterventionType;
import com.soulbuddy.global.enums.RiskLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassificationResult {

    private EmotionTag emotion;
    private RiskLevel risk;
    private InterventionType intervention;
}
