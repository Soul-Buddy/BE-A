package com.soulbuddy.domain.user.controller;

import com.soulbuddy.domain.user.dto.OnboardingRequest;
import com.soulbuddy.domain.user.dto.OnboardingResponse;
import com.soulbuddy.domain.user.dto.ProfileResponse;
import com.soulbuddy.domain.user.entity.Profile;
import com.soulbuddy.domain.user.service.ProfileService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile", description = "사용자 프로필 / 온보딩")
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "온보딩 제출", description = "온보딩 설문을 제출합니다 (최초 1회).")
    @PostMapping("/api/onboarding")
    public ResponseEntity<ApiResponse<OnboardingResponse>> onboarding(
            Authentication authentication,
            @Valid @RequestBody OnboardingRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        Profile profile = profileService.createProfile(userId, request);

        OnboardingResponse response = OnboardingResponse.builder()
                .profileId(profile.getId())
                .profileCompleted(true)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "프로필 조회", description = "현재 사용자의 프로필을 조회합니다.")
    @GetMapping("/api/profiles/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Profile profile = profileService.getProfile(userId);

        return ResponseEntity.ok(ApiResponse.success(toResponse(profile)));
    }

    @Operation(summary = "프로필 수정", description = "현재 사용자의 프로필을 수정합니다.")
    @PutMapping("/api/profiles/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody OnboardingRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        Profile profile = profileService.updateProfile(userId, request);

        return ResponseEntity.ok(ApiResponse.success(toResponse(profile)));
    }

    private ProfileResponse toResponse(Profile profile) {
        return ProfileResponse.builder()
                .profileId(profile.getId())
                .userId(profile.getUser().getId())
                .nickname(profile.getNickname())
                .hobbies(profile.getHobbies())
                .personality(profile.getPersonality())
                .concerns(profile.getConcerns())
                .preferredTone(profile.getPreferredTone().name())
                .likedThings(profile.getLikedThings())
                .dislikedThings(profile.getDislikedThings())
                .additionalInfo(profile.getAdditionalInfo())
                .build();
    }
}
