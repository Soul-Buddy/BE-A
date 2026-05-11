package com.soulbuddy.domain.safety.controller;

import com.soulbuddy.domain.safety.dto.SafetyDto;
import com.soulbuddy.domain.safety.service.SafetyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Safety", description = "위험 감지·Safety 이벤트")
@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    @Operation(summary = "위험도 분석 (키워드+AI 결과 결합)")
    @PostMapping("/detect")
    public ResponseEntity<SafetyDto.RiskDetectResponse> detectRisk(
            @RequestBody SafetyDto.RiskDetectRequest request) {
        return ResponseEntity.ok(safetyService.detectRisk(request));
    }

    @Operation(summary = "Safety 이벤트 기록 (배너 표시·센터 탭 등)")
    @PostMapping("/events")
    public ResponseEntity<SafetyDto.SafetyEventResponse> recordEvent(
            @RequestBody SafetyDto.SafetyEventRequest request) {
        return ResponseEntity.ok(safetyService.recordEvent(request));
    }

    @Operation(summary = "세션의 Safety 이벤트 목록 조회")
    @GetMapping("/events/session/{sessionId}")
    public ResponseEntity<List<SafetyDto.SafetyEventResponse>> getSessionEvents(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(safetyService.getSessionEvents(sessionId));
    }
}