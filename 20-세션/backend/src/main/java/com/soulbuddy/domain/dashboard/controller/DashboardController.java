package com.soulbuddy.domain.dashboard.controller;

import com.soulbuddy.domain.dashboard.dto.response.DashboardResponse;
import com.soulbuddy.domain.dashboard.service.DashboardService;
import com.soulbuddy.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal String principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getDashboard(userId, page, size)
        ));
    }
}
