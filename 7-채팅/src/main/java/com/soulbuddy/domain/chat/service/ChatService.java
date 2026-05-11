package com.soulbuddy.domain.chat.service;

import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.ai.service.AiChatService;
import com.soulbuddy.domain.chat.dto.response.ChatHistoryResponse;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.chat.repository.ChatMessageRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRepository;
import com.soulbuddy.domain.emotion.service.EmotionLogService;
import com.soulbuddy.domain.user.service.ProfileQueryService;
import com.soulbuddy.global.enums.*;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiChatService aiChatService;
    private final EmotionLogService emotionLogService;
    private final ProfileQueryService profileQueryService;
    private final com.soulbuddy.domain.safety.service.SafetyEventService safetyEventService;

    // POST /api/chat
    public ChatResponse processChat(Long userId, ChatRequest request) {

        ChatSession session = chatSessionRepository.findByIdAndUserId(request.getSessionId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_002);
        }

        // ① USER 메시지 저장 (분류 결과는 AI 호출 후 반영)
        ChatMessage userMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .sessionId(session.getId())
                        .sender(Sender.USER)
                        .content(request.getMessage())
                        .build()
        );

        // ② PromptContext 조립
        List<ChatMessage> recent = chatMessageRepository
                .findTop20BySessionIdOrderByCreatedAtDesc(session.getId());
        Collections.reverse(recent);

        boolean firstTurn = recent.stream().filter(m -> m.getSender() == Sender.USER).count() == 1;

        List<PromptContext.TurnMessage> recentTurns = recent.stream()
                .map(m -> PromptContext.TurnMessage.builder()
                        .role(m.getSender() == Sender.USER ? "user" : "assistant")
                        .content(m.getContent())
                        .build())
                .toList();

        PromptContext context = PromptContext.builder()
                .personaType(request.getPersonaType())
                .personalInstruction(profileQueryService.getPersonalInstructionByUserId(userId))
                .nickname(profileQueryService.getNicknameByUserId(userId))
                .recentSummary(request.getRecentSummary())
                .recentTurns(recentTurns)
                .firstTurn(firstTurn)
                .build();

        // ③ recentHighCount 산출
        long recentHighCount = chatMessageRepository
                .countBySessionIdAndRiskLevel(session.getId(), RiskLevel.HIGH);

        // ④ AI 호출
        ChatResponse response = aiChatService.process(request, context, recentHighCount);

        // ⑤ ASSISTANT 메시지 저장
        ChatMessage assistantMessage = chatMessageRepository.save(
                ChatMessage.builder()
                        .sessionId(session.getId())
                        .sender(response.isForcedSafety() ? Sender.SYSTEM : Sender.ASSISTANT)
                        .content(response.getAssistantMessage())
                        .emotionTag(response.getEmotionTag())
                        .riskLevel(response.getRiskLevel())
                        .interventionType(response.getInterventionType())
                        .ragUsed(response.isRagUsed())
                        .aiModel(response.getAiModel())
                        .build()
        );

        // ⑥ 감정 로그
        if (response.getEmotionTag() != null) {
            emotionLogService.log(userId, session.getId(),
                    userMessage.getId(), response.getEmotionTag(), EmotionSource.MESSAGE);
        }

        // ⑦ 위험도 HIGH → safety 이벤트
        if (response.getRiskLevel() == RiskLevel.HIGH) {
            safetyEventService.recordRiskDetected(
                    userId,
                    session.getId(),
                    userMessage.getId(),
                    RiskLevel.HIGH,
                    false
            );
        }

        // ⑧ 강제 안전 응답 → safety 이벤트
        if (response.isForcedSafety()) {
            safetyEventService.recordForcedSafetyReply(
                    userId,
                    session.getId(),
                    assistantMessage.getId(),
                    response.getRiskLevel(),
                    true
            );
        }

        return response;
    }

    // GET /api/chat/history/{sessionId}
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(Long userId, String sessionId, int page, int size) {

        chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));

        Page<ChatMessage> messagePage = chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId, PageRequest.of(page, size));

        List<ChatHistoryResponse.MessageItem> items = messagePage.getContent().stream()
                .map(m -> ChatHistoryResponse.MessageItem.builder()
                        .messageId(m.getId())
                        .sender(m.getSender())
                        .content(m.getContent())
                        .emotionTag(m.getEmotionTag())
                        .riskLevel(m.getRiskLevel())
                        .interventionType(m.getInterventionType())
                        .ragUsed(m.isRagUsed())
                        .aiModel(m.getAiModel())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();

        return ChatHistoryResponse.builder()
                .sessionId(sessionId)
                .messages(items)
                .totalCount(messagePage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }
}
