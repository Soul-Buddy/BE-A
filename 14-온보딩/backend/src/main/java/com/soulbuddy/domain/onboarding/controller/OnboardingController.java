package com.soulbuddy.domain.onboarding.controller;

import com.soulbuddy.domain.onboarding.dto.OnboardingRequestDto;
import com.soulbuddy.domain.onboarding.dto.OnboardingResponseDto;
import com.soulbuddy.domain.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * 온보딩 정보 저장 및 프로필 생성
     * POST /api/onboarding
     */
    @PostMapping
    public ResponseEntity<OnboardingResponseDto> saveOnboarding(
            @RequestBody @Valid OnboardingRequestDto requestDto) {

        OnboardingResponseDto response = onboardingService.saveOnboarding(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 사용자의 온보딩 완료 여부나 프로필을 조회해야 할 경우 추가
     * GET /api/onboarding/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<OnboardingResponseDto> getOnboardingProfile(@PathVariable Long userId) {
        // (필요 시 서비스에 조회 로직 추가 후 호출)
        // OnboardingResponseDto response = onboardingService.getOnboardingByUserId(userId);
        // return ResponseEntity.ok(response);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}