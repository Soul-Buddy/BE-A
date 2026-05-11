package com.soulbuddy.domain.user.controller;

import com.soulbuddy.domain.user.dto.UserMeResponse;
import com.soulbuddy.domain.user.dto.UserSettingsResponse;
import com.soulbuddy.domain.user.dto.UserSettingsUpdateRequest;
import com.soulbuddy.domain.user.service.UserService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(
            @AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getMe(Long.parseLong(principal))));
    }

    @Operation(summary = "알림 설정 조회")
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getSettings(
            @AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getSettings(Long.parseLong(principal))));
    }

    @Operation(summary = "알림 설정 변경")
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
            @AuthenticationPrincipal String principal,
            @RequestBody UserSettingsUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateSettings(Long.parseLong(principal), request)));
    }
}
