package com.soulbuddy.domain.chat.controller;

import com.soulbuddy.domain.chat.dto.*;
import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.chat.service.SessionService;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Session", description = "채팅 세션 관리")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @Operation(summary = "새 채팅 세션 시작", description = "페르소나를 선택하여 새 세션을 시작합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<SessionCreateResponse>> createSession(
            Authentication authentication,
            @Valid @RequestBody SessionCreateRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        ChatSession session = sessionService.createSession(userId, request.getPersonaType());
        String recentSummary = sessionService.buildRecentSummary(userId);

        SessionCreateResponse response = SessionCreateResponse.builder()
                .sessionId(session.getId())
                .personaType(session.getPersonaType().name())
                .recentSummary(recentSummary)
                .createdAt(session.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세션 목록 조회", description = "현재 사용자의 세션 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<SessionListResponse>> getSessions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = Long.parseLong(authentication.getName());
        size = Math.min(size, 50);
        Page<ChatSession> sessionPage = sessionService.getSessions(userId, PageRequest.of(page, size));

        List<SessionListResponse.SessionItem> items = sessionPage.getContent().stream()
                .map(s -> SessionListResponse.SessionItem.builder()
                        .sessionId(s.getId())
                        .personaType(s.getPersonaType().name())
                        .status(s.getStatus().name())
                        .title(s.getTitle())
                        .startedAt(s.getStartedAt())
                        .endedAt(s.getEndedAt())
                        .build())
                .toList();

        SessionListResponse response = SessionListResponse.builder()
                .sessions(items)
                .totalCount(sessionPage.getTotalElements())
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세션 종료", description = "세션을 종료하고 요약을 생성합니다.")
    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<ApiResponse<SessionEndResponse>> endSession(
            Authentication authentication,
            @PathVariable String sessionId) {
        Long userId = Long.parseLong(authentication.getName());
        Summary summary = sessionService.endSession(userId, sessionId);

        SessionEndResponse response = SessionEndResponse.builder()
                .sessionId(sessionId)
                .summary(summary.getSummaryText())
                .dominantEmotion(summary.getDominantEmotion() != null
                        ? summary.getDominantEmotion().name() : null)
                .memoryHint(summary.getMemoryHint())
                .endedAt(summary.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
