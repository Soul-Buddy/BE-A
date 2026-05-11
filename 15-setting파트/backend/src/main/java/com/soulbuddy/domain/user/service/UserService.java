package com.soulbuddy.domain.user.service;

import com.soulbuddy.domain.user.dto.UserProfileResponse;
import com.soulbuddy.domain.user.dto.UserProfileUpdateRequest;
import com.soulbuddy.global.enums.Gender; // 1. 글로벌 패키지 import 로 변경
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return UserProfileResponse.builder()
                .nickname(user.getNickname())
                .gender(user.getGender().name())
                .job(user.getJob())
                .ageGroup(user.getAgeGroup())
                .profileImageUrl(user.getProfileImageUrl())
                .dailyCheckInAlarm(user.isDailyCheckInAlarm())
                .cheerMessageAlarm(user.isCheerMessageAlarm())
                .build();
    }

    @Transactional
    public void updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.setNickname(request.getNickname());
        user.setGender(Gender.valueOf(request.getGender().toUpperCase()));

        user.setJob(request.getJob());
        user.setAgeGroup(request.getAgeGroup());
        user.setDailyCheckInAlarm(request.isDailyCheckInAlarm());
        user.setCheerMessageAlarm(request.isCheerMessageAlarm());
    }
}