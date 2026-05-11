package com.soulbuddy.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AgreementResponse {

    private Long userId;
    private String email;
    private String nickname;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime termsAgreedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime privacyAgreedAt;

    private boolean agreedAll;

    public static AgreementResponse of(Long userId, String email, String nickname,
                                       LocalDateTime termsAgreedAt, LocalDateTime privacyAgreedAt) {
        return AgreementResponse.builder()
                .userId(userId)
                .email(email)
                .nickname(nickname)
                .termsAgreedAt(termsAgreedAt)
                .privacyAgreedAt(privacyAgreedAt)
                .agreedAll(termsAgreedAt != null && privacyAgreedAt != null)
                .build();
    }
}