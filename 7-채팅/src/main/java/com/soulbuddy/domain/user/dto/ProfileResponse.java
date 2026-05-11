package com.soulbuddy.domain.user.dto;

import com.soulbuddy.domain.user.entity.Profile;
import com.soulbuddy.global.enums.Gender;
import com.soulbuddy.global.enums.PreferredTone;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProfileResponse {
    private Long profileId;
    private Long userId;
    private String nickname;
    private Integer age;
    private Gender gender;
    private String occupation;
    private String usageIntent;
    private List<String> hobbies;
    private PreferredTone preferredTone;
    private List<String> likedThings;
    private List<String> dislikedThings;

    public static ProfileResponse from(Profile profile) {
        return ProfileResponse.builder()
                .profileId(profile.getId())
                .userId(profile.getUser().getId())
                .nickname(profile.getNickname())
                .age(profile.getAge())
                .gender(profile.getGender())
                .occupation(profile.getOccupation())
                .usageIntent(profile.getUsageIntent())
                .hobbies(profile.getHobbies())
                .preferredTone(profile.getPreferredTone())
                .likedThings(profile.getLikedThings())
                .dislikedThings(profile.getDislikedThings())
                .build();
    }
}
