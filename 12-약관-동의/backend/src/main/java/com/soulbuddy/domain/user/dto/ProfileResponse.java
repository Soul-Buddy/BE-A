package com.soulbuddy.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProfileResponse {

    private Long profileId;

    private Long userId;

    private String nickname;

    private List<String> hobbies;

    private String personality;

    private List<String> concerns;

    private String preferredTone;

    private List<String> likedThings;

    private List<String> dislikedThings;

    private String additionalInfo;
}
