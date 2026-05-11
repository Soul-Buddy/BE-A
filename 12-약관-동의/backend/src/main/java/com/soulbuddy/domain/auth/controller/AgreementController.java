package com.soulbuddy.domain.auth.controller;

import com.soulbuddy.domain.auth.dto.AgreementRequest;
import com.soulbuddy.domain.auth.dto.AgreementResponse;
import com.soulbuddy.domain.auth.service.AgreementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agreement")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    @PostMapping
    public ResponseEntity<AgreementResponse> agree(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AgreementRequest request
    ) {
        return ResponseEntity.ok(agreementService.agree(userId, request));
    }

    @GetMapping
    public ResponseEntity<AgreementResponse> getStatus(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(agreementService.getAgreementStatus(userId));
    }
}