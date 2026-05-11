package com.soulbuddy.domain.auth.controller;

import com.soulbuddy.domain.auth.dto.GoogleLoginRequest;
import com.soulbuddy.domain.auth.dto.LoginResponse;
import com.soulbuddy.domain.auth.dto.TokenRefreshRequest;
import com.soulbuddy.domain.auth.dto.TokenRefreshResponse;
import com.soulbuddy.domain.auth.service.AuthService;
import com.soulbuddy.global.auth.JwtTokenProvider;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ApiResponse;
import com.soulbuddy.global.response.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Google 로그인", description = "FE가 NextAuth로 획득한 Google ID Token을 검증하고 서비스 JWT를 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithGoogle(request.getGoogleIdToken())));
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access Token을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {
        if (!jwtTokenProvider.validate(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        Long userId = jwtTokenProvider.getUserId(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(
                TokenRefreshResponse.builder()
                        .accessToken(jwtTokenProvider.createAccessToken(userId))
                        .build()
        ));
    }
}
