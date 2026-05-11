package com.soulbuddy.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OnboardingResponse {

    private Long profileId;

    private boolean profileCompleted;
}
