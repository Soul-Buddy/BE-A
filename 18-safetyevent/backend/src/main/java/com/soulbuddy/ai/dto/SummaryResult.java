package com.soulbuddy.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummaryResult {

    private String summaryText;

    private String dominantEmotion;

    private String memoryHint;
}
