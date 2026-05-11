package com.soulbuddy.domain.user.controller;

import com.soulbuddy.domain.user.dto.OnboardingRequest;
import com.soulbuddy.domain.user.dto.OnboardingResponse;
import com.soulbuddy.domain.user.dto.ProfileResponse;
import com.soulbuddy.domain.user.dto.ProfileUpdateRequest;
import com.soulbuddy.domain.user.service.ProfileService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile", description = "프로필·온보딩")
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "온보딩 제출")
    @PostMapping("/api/onboarding")
    public ResponseEntity<ApiResponse<OnboardingResponse>> onboard(
            @AuthenticationPrincipal String principal,
            @Valid @RequestBody OnboardingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.onboard(Long.parseLong(principal), request)));
    }

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/api/profiles/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.getProfile(Long.parseLong(principal))));
    }

    @Operation(summary = "내 프로필 수정")
    @PutMapping("/api/profiles/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal String principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.updateProfile(Long.parseLong(principal), request)));
    }
}
