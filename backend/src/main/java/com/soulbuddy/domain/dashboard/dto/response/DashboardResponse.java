package com.soulbuddy.domain.dashboard.dto.response;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.PersonaType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DashboardResponse {

    private Long userId;
    private Map<EmotionTag, Long> emotionStats;
    private List<SummaryCard> recentSummaries;
    private long totalSummaryCount;
    private int page;
    private int size;

    @Getter
    @Builder
    public static class SummaryCard {
        private String sessionId;
        private LocalDate date;
        private PersonaType personaType;
        private String characterName;
        private String quoteText;
        private EmotionTag dominantEmotion;
        private String emotionChange;
    }
}
