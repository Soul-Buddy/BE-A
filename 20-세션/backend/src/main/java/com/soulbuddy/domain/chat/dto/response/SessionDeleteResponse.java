package com.soulbuddy.domain.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionDeleteResponse {

    private String sessionId;
    private LocalDateTime deletedAt;
}
