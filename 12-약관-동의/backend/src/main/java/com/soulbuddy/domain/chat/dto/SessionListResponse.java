package com.soulbuddy.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SessionListResponse {

    private List<SessionItem> sessions;

    private long totalCount;

    private int page;

    private int size;

    @Getter
    @Builder
    public static class SessionItem {
        private String sessionId;
        private String personaType;
        private String status;
        private String title;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
    }
}
