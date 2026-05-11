package com.soulbuddy.domain.user.dto;

import com.soulbuddy.global.enums.Gender;
import com.soulbuddy.global.enums.PreferredTone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class OnboardingRequest {
    @NotBlank
    private String nickname;
    private Integer age;
    private Gender gender;
    private String occupation;
    private String usageIntent;
    private List<String> hobbies;
    @NotNull
    private PreferredTone preferredTone;
    private List<String> likedThings;
    private List<String> dislikedThings;
}
