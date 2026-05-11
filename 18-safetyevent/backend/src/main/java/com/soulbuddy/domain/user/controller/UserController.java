package com.soulbuddy.domain.user.controller;

import com.soulbuddy.domain.user.dto.UserMeResponse;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.service.ProfileService;
import com.soulbuddy.domain.user.service.UserService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 정보")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 기본 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        User user = userService.getUser(userId);

        UserMeResponse response = UserMeResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileCompleted(profileService.hasProfile(userId))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
