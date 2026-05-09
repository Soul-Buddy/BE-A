package com.soulbuddy.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private String nickname;
    private String gender;
    private String job;
    private String ageGroup;
    private String profileImageUrl;
    private boolean dailyCheckInAlarm;
    private boolean cheerMessageAlarm;
    private int historyCount;
}
