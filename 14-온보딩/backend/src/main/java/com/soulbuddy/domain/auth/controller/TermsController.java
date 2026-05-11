package com.soulbuddy.domain.auth.controller;

import com.soulbuddy.domain.auth.dto.TermsAgreementRequest;
import com.soulbuddy.domain.auth.dto.TermsAgreementResponse;
import com.soulbuddy.domain.auth.service.TermsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    // 약관 동의
    @PostMapping("/{userId}")
    public ResponseEntity<TermsAgreementResponse> agreeTerms(
            @PathVariable Long userId,
            @RequestBody TermsAgreementRequest request) {

        return ResponseEntity.ok(termsService.agreeTerms(userId, request));
    }

    // 약관 동의 여부 확인
    @GetMapping("/{userId}/check")
    public ResponseEntity<Boolean> checkTerms(@PathVariable Long userId) {
        return ResponseEntity.ok(termsService.checkTermsAgreed(userId));
    }
}