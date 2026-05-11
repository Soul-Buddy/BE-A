package com.soulbuddy.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OnboardingResponse {
    private Long profileId;
    private LocalDateTime onboardingCompletedAt;
}
