package com.soulbuddy.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserProfileUpdateRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    private String gender;
    private String job;
    private String ageGroup;
    private boolean dailyCheckInAlarm;
    private boolean cheerMessageAlarm;
}

