package com.soulbuddy.domain.user.service;

import com.soulbuddy.domain.user.dto.UserMeResponse;
import com.soulbuddy.domain.user.dto.UserSettingsResponse;
import com.soulbuddy.domain.user.dto.UserSettingsUpdateRequest;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.entity.UserSettings;
import com.soulbuddy.domain.user.repository.ProfileRepository;
import com.soulbuddy.domain.user.repository.UserRepository;
import com.soulbuddy.domain.user.repository.UserSettingsRepository;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Transactional(readOnly = true)
    public UserMeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        boolean onboardingCompleted = profileRepository.findByUserId(userId)
                .map(p -> p.getOnboardingCompletedAt() != null)
                .orElse(false);
        return UserMeResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .termsAgreed(user.hasAgreedAll())
                .onboardingCompleted(onboardingCompleted)
                .build();
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse getSettings(Long userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        return UserSettingsResponse.builder()
                .safetyResourceConsent(settings.isSafetyResourceConsent())
                .build();
    }

    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UserSettingsUpdateRequest request) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        settings.update(request.isSafetyResourceConsent());
        return UserSettingsResponse.builder()
                .safetyResourceConsent(settings.isSafetyResourceConsent())
                .build();
    }
}
