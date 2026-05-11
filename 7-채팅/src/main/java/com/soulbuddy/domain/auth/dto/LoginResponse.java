package com.soulbuddy.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long userId;
    private boolean isNewUser;
    private boolean termsAgreed;
    private boolean onboardingCompleted;
}
