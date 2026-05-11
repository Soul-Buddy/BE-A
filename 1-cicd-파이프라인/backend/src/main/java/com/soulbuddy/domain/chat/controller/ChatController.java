package com.soulbuddy.domain.chat.controller;

import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.domain.chat.dto.ChatHistoryResponse;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.domain.chat.service.ChatService;
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

@Tag(name = "Chat", description = "AI 채팅")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "AI에게 메시지 전송", description = "AI에게 메시지를 전송하고 응답을 수신합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            Authentication authentication,
            @Valid @RequestBody ChatRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        ChatResponse response = chatService.processChat(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대화 기록 조회", description = "특정 세션의 대화 기록을 조회합니다.")
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getChatHistory(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        size = Math.min(size, 100);
        Page<ChatMessage> messagePage = chatService.getChatHistory(sessionId, PageRequest.of(page, size));

        List<ChatHistoryResponse.MessageItem> items = messagePage.getContent().stream()
                .map(msg -> ChatHistoryResponse.MessageItem.builder()
                        .messageId(msg.getId())
                        .sender(msg.getSender().name())
                        .content(msg.getContent())
                        .emotionTag(msg.getEmotionTag() != null ? msg.getEmotionTag().name() : null)
                        .createdAt(msg.getCreatedAt())
                        .build())
                .toList();

        ChatHistoryResponse response = ChatHistoryResponse.builder()
                .sessionId(sessionId)
                .messages(items)
                .totalCount(messagePage.getTotalElements())
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
