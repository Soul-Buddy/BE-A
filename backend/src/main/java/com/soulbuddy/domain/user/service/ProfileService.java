package com.soulbuddy.domain.user.service;

import com.soulbuddy.domain.user.dto.OnboardingRequest;
import com.soulbuddy.domain.user.dto.OnboardingResponse;
import com.soulbuddy.domain.user.dto.ProfileResponse;
import com.soulbuddy.domain.user.dto.ProfileUpdateRequest;
import com.soulbuddy.domain.user.entity.Profile;
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
public class ProfileService implements ProfileQueryService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public String getNicknameByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(Profile::getNickname)
                .orElse("사용자");
    }

    @Override
    @Transactional(readOnly = true)
    public String getPersonalInstructionByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(Profile::getPersonalInstruction)
                .orElse("");
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));
        return ProfileResponse.from(profile);
    }

    @Transactional
    public OnboardingResponse onboard(Long userId, OnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        Profile profile = profileRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            profile = Profile.builder()
                    .user(user)
                    .nickname(request.getNickname())
                    .age(request.getAge())
                    .gender(request.getGender())
                    .occupation(request.getOccupation())
                    .usageIntent(request.getUsageIntent())
                    .hobbies(request.getHobbies())
                    .preferredTone(request.getPreferredTone())
                    .likedThings(request.getLikedThings())
                    .dislikedThings(request.getDislikedThings())
                    .build();
            profile.completeOnboarding();
            profile = profileRepository.save(profile);
        } else if (profile.getOnboardingCompletedAt() == null) {
            profile.update(request.getNickname(), request.getAge(), request.getGender(),
                    request.getOccupation(), request.getUsageIntent(), request.getHobbies(),
                    request.getPreferredTone(), request.getLikedThings(), request.getDislikedThings());
            profile.completeOnboarding();
        }

        userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> userSettingsRepository.save(UserSettings.defaultFor(user)));

        return OnboardingResponse.builder()
                .profileId(profile.getId())
                .onboardingCompletedAt(profile.getOnboardingCompletedAt())
                .build();
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));
        profile.update(request.getNickname(), request.getAge(), request.getGender(),
                request.getOccupation(), request.getUsageIntent(), request.getHobbies(),
                request.getPreferredTone(), request.getLikedThings(), request.getDislikedThings());
        return ProfileResponse.from(profile);
    }
}
