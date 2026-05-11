package com.soulbuddy.domain.auth.service;

import com.soulbuddy.domain.auth.dto.TermsAgreementRequest;
import com.soulbuddy.domain.auth.dto.TermsAgreementResponse;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.repository.UserRepository;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final UserRepository userRepository;

    @Transactional
    public TermsAgreementResponse agreeTerms(Long userId, TermsAgreementRequest request) {

        if (!request.isAllAgreed()) {
            throw new BusinessException(ErrorCode.TERMS_001);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        if (user.hasAgreedTerms()) {
            return TermsAgreementResponse.builder()
                    .userId(userId)
                    .agreed(true)
                    .termsAgreedAt(user.getTermsAgreedAt())
                    .nextStep("ONBOARDING")
                    .build();
        }

        user.agreeTerms();

        return TermsAgreementResponse.builder()
                .userId(userId)
                .agreed(true)
                .termsAgreedAt(user.getTermsAgreedAt())
                .nextStep("ONBOARDING")
                .build();
    }

    @Transactional(readOnly = true)
    public boolean checkTermsAgreed(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        return user.hasAgreedTerms();
    }
}