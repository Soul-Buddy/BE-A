package com.soulbuddy.domain.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// GET /api/sessions 페이지네이션 응답 wrapper
@Getter
@Builder
public class SessionListResponse {

    private List<SessionItemResponse> sessions;
    private long totalCount;
    private int page;
    private int size;
}
