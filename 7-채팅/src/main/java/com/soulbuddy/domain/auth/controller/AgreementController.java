package com.soulbuddy.domain.auth.controller;

import com.soulbuddy.domain.auth.dto.AgreementRequest;
import com.soulbuddy.domain.auth.dto.AgreementResponse;
import com.soulbuddy.domain.auth.service.AgreementService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Agreement", description = "약관 동의")
@RestController
@RequestMapping("/api/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    @Operation(summary = "약관 동의 제출")
    @PostMapping
    public ResponseEntity<ApiResponse<AgreementResponse>> agree(
            @AuthenticationPrincipal String principal,
            @Valid @RequestBody AgreementRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.agree(Long.parseLong(principal), request)));
    }

    @Operation(summary = "약관 동의 상태 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<AgreementResponse>> getStatus(
            @AuthenticationPrincipal String principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.getAgreementStatus(Long.parseLong(principal))));
    }
}