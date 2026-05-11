package com.soulbuddy.domain.user.service;

import com.soulbuddy.domain.user.dto.OnboardingRequest;
import com.soulbuddy.domain.user.entity.Profile;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.repository.ProfileRepository;
import com.soulbuddy.domain.user.repository.UserRepository;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional
    public Profile createProfile(Long userId, OnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        Profile profile = Profile.builder()
                .user(user)
                .nickname(request.getNickname())
                .hobbies(request.getHobbies())
                .personality(request.getPersonality())
                .concerns(request.getConcerns())
                .preferredTone(request.getPreferredTone())
                .likedThings(request.getLikedThings())
                .dislikedThings(request.getDislikedThings())
                .additionalInfo(request.getAdditionalInfo())
                .build();

        return profileRepository.save(profile);
    }

    public Profile getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));
    }

    @Transactional
    public Profile updateProfile(Long userId, OnboardingRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));

        Profile updated = Profile.builder()
                .id(profile.getId())
                .user(profile.getUser())
                .nickname(request.getNickname())
                .hobbies(request.getHobbies())
                .personality(request.getPersonality())
                .concerns(request.getConcerns())
                .preferredTone(request.getPreferredTone())
                .likedThings(request.getLikedThings())
                .dislikedThings(request.getDislikedThings())
                .additionalInfo(request.getAdditionalInfo())
                .build();

        return profileRepository.save(updated);
    }

    public boolean hasProfile(Long userId) {
        return profileRepository.existsByUserId(userId);
    }
}
