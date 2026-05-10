package com.soulbuddy.domain.onboarding.dto;

import com.soulbuddy.global.enums.Gender;
import com.soulbuddy.global.enums.PreferredTone;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingRequestDto {

    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

    @Min(value = 10, message = "올바른 나이를 입력해주세요")
    private Integer age;

    private Gender gender;

    @Size(max = 100, message = "직업은 100자 이하여야 합니다")
    private String occupation;

    private String usageIntent;

    @Size(max = 3, message = "취미는 최대 3개까지 선택 가능합니다")
    private List<String> hobbies;

    @NotNull(message = "원하는 대화 톤을 선택해주세요")
    private PreferredTone preferredTone;
    private List<String> likedThings;

    private List<String> dislikedThings;
}