package com.soulbuddy.domain.auth.service;

import com.soulbuddy.domain.auth.dto.AgreementRequest;
import com.soulbuddy.domain.auth.dto.AgreementResponse;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgreementService {

    private final UserRepository userRepository;

    @Transactional
    public AgreementResponse agree(Long userId, AgreementRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));

        if (!Boolean.TRUE.equals(request.getTermsAgreed())) {
            throw new IllegalArgumentException("서비스 이용약관 동의는 필수입니다.");
        }
        if (!Boolean.TRUE.equals(request.getPrivacyAgreed())) {
            throw new IllegalArgumentException("개인정보 수집·이용 동의는 필수입니다.");
        }

        if (user.getTermsAgreedAt() != null) {
            return AgreementResponse.of(
                    user.getId(), user.getEmail(), user.getNickname(),
                    user.getTermsAgreedAt(), user.getPrivacyAgreedAt()
            );
        }

        LocalDateTime now = LocalDateTime.now();
        user.agreeTerms(now);
        user.agreePrivacy(now);
        User saved = userRepository.save(user);

        return AgreementResponse.of(
                saved.getId(), saved.getEmail(), saved.getNickname(),
                saved.getTermsAgreedAt(), saved.getPrivacyAgreedAt()
        );
    }

    @Transactional(readOnly = true)
    public AgreementResponse getAgreementStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));

        return AgreementResponse.of(
                user.getId(), user.getEmail(), user.getNickname(),
                user.getTermsAgreedAt(), user.getPrivacyAgreedAt()
        );
    }
}