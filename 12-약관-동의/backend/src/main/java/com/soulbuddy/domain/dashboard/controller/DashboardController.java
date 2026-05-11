package com.soulbuddy.domain.dashboard.controller;

import com.soulbuddy.domain.dashboard.dto.DashboardResponse;
import com.soulbuddy.domain.dashboard.service.DashboardService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "감정 이력 + 요약 대시보드")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 조회", description = "감정 통계와 최근 요약 카드를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = Long.parseLong(authentication.getName());
        size = Math.min(size, 30);
        DashboardResponse response = dashboardService.getDashboard(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
