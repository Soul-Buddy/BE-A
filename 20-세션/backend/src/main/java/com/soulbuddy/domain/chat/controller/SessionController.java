package com.soulbuddy.domain.chat.controller;

import com.soulbuddy.domain.chat.dto.request.PreChatEmotionRequest;
import com.soulbuddy.domain.chat.dto.request.SessionCreateRequest;
import com.soulbuddy.domain.chat.dto.response.PreChatEmotionResponse;
import com.soulbuddy.domain.chat.dto.response.SessionCreateResponse;
import com.soulbuddy.domain.chat.dto.response.SessionDeleteResponse;
import com.soulbuddy.domain.chat.dto.response.SessionEndResponse;
import com.soulbuddy.domain.chat.dto.response.SessionListResponse;
import com.soulbuddy.domain.chat.service.SessionService;
import com.soulbuddy.domain.user.service.ProfileQueryService;
import com.soulbuddy.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final ProfileQueryService profileQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<SessionListResponse>> getSessions(
            @AuthenticationPrincipal String principal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.getSessions(userId, status, page, size)
        ));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<SessionDeleteResponse>> deleteSession(
            @AuthenticationPrincipal String principal,
            @PathVariable String sessionId) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.deleteSession(userId, sessionId)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SessionCreateResponse>> createSession(
            @AuthenticationPrincipal String principal,
            @Valid @RequestBody SessionCreateRequest request) {

        Long userId = Long.parseLong(principal);
        String nickname = profileQueryService.getNicknameByUserId(userId);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.createSession(userId, request.getPersonaType(), nickname)
        ));
    }

    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<ApiResponse<SessionEndResponse>> endSession(
            @AuthenticationPrincipal String principal,
            @PathVariable String sessionId) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.endSession(userId, sessionId)
        ));
    }

    @PatchMapping("/{sessionId}/pre-chat-emotion")
    public ResponseEntity<ApiResponse<PreChatEmotionResponse>> updatePreChatEmotion(
            @AuthenticationPrincipal String principal,
            @PathVariable String sessionId,
            @Valid @RequestBody PreChatEmotionRequest request) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.updatePreChatEmotion(userId, sessionId, request.getPreChatEmotion())
        ));
    }
}
