package com.soulbuddy.domain.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DashboardResponse {

    private Long userId;

    private Map<String, Long> emotionStats;

    private List<SummaryCard> recentSummaries;

    private long totalSummaryCount;

    private int page;

    private int size;

    @Getter
    @Builder
    public static class SummaryCard {
        private String sessionId;
        private String date;
        private String summary;
        private String dominantEmotion;
        private String personaType;
    }
}
