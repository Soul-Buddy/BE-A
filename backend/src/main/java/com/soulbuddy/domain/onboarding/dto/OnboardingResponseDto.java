package com.soulbuddy.domain.onboarding.dto;

import com.soulbuddy.global.enums.Gender;
import com.soulbuddy.global.enums.PreferredTone;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResponseDto {

    private Long id; // profiles 테이블의 PK (id)
    private Long userId; // 연결된 사용자의 ID (user_id)
    private String nickname; // 온보딩 입력 닉네임
    private Integer age;
    private Gender gender;
    private String occupation;
    private String usageIntent;
    private List<String> hobbies;
    private PreferredTone preferredTone;
    private List<String> likedThings;
    private List<String> dislikedThings;
    private String personalInstruction;
    private LocalDateTime onboardingCompletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}