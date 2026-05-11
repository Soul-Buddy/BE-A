package com.soulbuddy.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserMeResponse {

    private Long userId;

    private String nickname;

    private String email;

    private boolean profileCompleted;
}
