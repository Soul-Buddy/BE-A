package com.soulbuddy.domain.summary.dto;

import com.soulbuddy.global.enums.EmotionTag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummaryCardDto {

    private String quoteText;
    private EmotionTag dominantEmotion;
    private String emotionChange;
}
