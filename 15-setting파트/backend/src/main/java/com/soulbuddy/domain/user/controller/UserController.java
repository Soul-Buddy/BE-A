package com.soulbuddy.domain.user.controller;

import com.soulbuddy.domain.user.dto.UserProfileResponse;
import com.soulbuddy.domain.user.dto.UserProfileUpdateRequest;
import com.soulbuddy.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestParam Long userId) {  // 임시: JWT 없을 때

        UserProfileResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @RequestParam Long userId,
            @RequestBody @Valid UserProfileUpdateRequest request) {

        userService.updateProfile(userId, request);
        return ResponseEntity.ok().build();
    }
}
