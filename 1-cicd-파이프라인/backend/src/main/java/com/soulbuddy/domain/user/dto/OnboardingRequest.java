package com.soulbuddy.domain.user.dto;

import com.soulbuddy.global.enums.PreferredTone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class OnboardingRequest {

    @NotBlank
    private String nickname;

    private List<String> hobbies;

    @Size(max = 200)
    private String personality;

    private List<String> concerns;

    @NotNull
    private PreferredTone preferredTone;

    private List<String> likedThings;

    private List<String> dislikedThings;

    private String additionalInfo;
}
