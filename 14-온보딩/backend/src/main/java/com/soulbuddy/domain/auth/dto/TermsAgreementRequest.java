package com.soulbuddy.domain.auth.dto;

import lombok.Getter;

@Getter
public class TermsAgreementRequest {
    private boolean allAgreed; // 세 가지 모두 이해했어요 (필수 체크박스)
}