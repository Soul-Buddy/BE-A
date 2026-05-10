package com.soulbuddy.domain.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper; // 추가됨
import com.soulbuddy.domain.onboarding.dto.OnboardingRequestDto;
import com.soulbuddy.domain.onboarding.dto.OnboardingResponseDto;
import com.soulbuddy.domain.onboarding.entity.OnboardingEntity;
import com.soulbuddy.domain.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingRepository onboardingRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OnboardingResponseDto saveOnboarding(OnboardingRequestDto dto) {

        String hobbiesJson = convertToJson(dto.getHobbies());
        String likedThingsJson = convertToJson(dto.getLikedThings());
        String dislikedThingsJson = convertToJson(dto.getDislikedThings());


        OnboardingEntity entity = OnboardingEntity.builder()
                .userId(dto.getUserId())
                .nickname(dto.getNickname())
                .age(dto.getAge())
                .gender(dto.getGender())
                .occupation(dto.getOccupation())
                .usageIntent(dto.getUsageIntent())
                .hobbies(hobbiesJson)
                .preferredTone(dto.getPreferredTone())
                .likedThings(likedThingsJson)
                .dislikedThings(dislikedThingsJson)
                .personalInstruction(generateInstruction(dto))
                .onboardingCompletedAt(LocalDateTime.now())
                .build();

        OnboardingEntity saved = onboardingRepository.save(entity);

        return OnboardingResponseDto.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .nickname(saved.getNickname())
                .age(saved.getAge())
                .gender(saved.getGender())
                .occupation(saved.getOccupation())
                .usageIntent(saved.getUsageIntent())
                .hobbies(dto.getHobbies()) // 원본 리스트 사용
                .preferredTone(saved.getPreferredTone())
                .likedThings(dto.getLikedThings())
                .dislikedThings(dto.getDislikedThings())
                .onboardingCompletedAt(saved.getOnboardingCompletedAt())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    private String generateInstruction(OnboardingRequestDto dto) {
        return String.format("사용자 닉네임은 %s이고, 직업은 %s입니다. 성향은 %s이며, 좋아하는 것은 %s입니다.",
                dto.getNickname(), dto.getOccupation(), dto.getPreferredTone(), dto.getLikedThings());
    }

    private String convertToJson(List<String> list) {
        try {
            return (list != null && !list.isEmpty()) ? objectMapper.writeValueAsString(list) : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 실패: " + e.getMessage());
        }
    }
}