package com.soulbuddy.domain.safety.controller;

import com.soulbuddy.domain.safety.dto.SafetyDto;
import com.soulbuddy.domain.safety.service.SafetyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    @PostMapping("/detect")
    public ResponseEntity<SafetyDto.RiskDetectResponse> detectRisk(
            @RequestBody SafetyDto.RiskDetectRequest request) {
        return ResponseEntity.ok(safetyService.detectRisk(request));
    }

    @PostMapping("/events")
    public ResponseEntity<SafetyDto.SafetyEventResponse> recordEvent(
            @RequestBody SafetyDto.SafetyEventRequest request) {
        return ResponseEntity.ok(safetyService.recordEvent(request));
    }

    @GetMapping("/events/session/{sessionId}")
    public ResponseEntity<List<SafetyDto.SafetyEventResponse>> getSessionEvents(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(safetyService.getSessionEvents(sessionId));
    }
}