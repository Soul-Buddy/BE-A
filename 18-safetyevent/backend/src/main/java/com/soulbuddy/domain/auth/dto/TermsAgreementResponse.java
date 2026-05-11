package com.soulbuddy.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class TermsAgreementResponse {
    private Long userId;
    private boolean agreed;
    private LocalDateTime termsAgreedAt;
    private String nextStep; // "ONBOARDING"
}