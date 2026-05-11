package com.soulbuddy.domain.chat.controller;

import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.domain.chat.dto.response.ChatHistoryResponse;
import com.soulbuddy.domain.chat.service.ChatService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "채팅 메시지")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "메시지 전송")
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @AuthenticationPrincipal String principal,
            @Valid @RequestBody ChatRequest request) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                chatService.processChat(userId, request)
        ));
    }

    @Operation(summary = "대화 히스토리 조회")
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getChatHistory(
            @AuthenticationPrincipal String principal,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                chatService.getChatHistory(userId, sessionId, page, size)
        ));
    }
}
