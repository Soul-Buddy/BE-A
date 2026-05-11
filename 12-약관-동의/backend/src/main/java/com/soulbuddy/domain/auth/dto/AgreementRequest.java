package com.soulbuddy.domain.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AgreementRequest {

    @NotNull(message = "서비스 이용약관 동의는 필수입니다.")
    private Boolean termsAgreed;

    @NotNull(message = "개인정보 수집·이용 동의는 필수입니다.")
    private Boolean privacyAgreed;
}