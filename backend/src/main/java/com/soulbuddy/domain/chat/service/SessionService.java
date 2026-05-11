package com.soulbuddy.domain.chat.service;

import com.soulbuddy.ai.dto.ChatMessageDto;
import com.soulbuddy.ai.dto.OpeningContext;
import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.ai.service.AiChatService;
import com.soulbuddy.ai.service.AiSummaryService;
import com.soulbuddy.domain.chat.dto.response.SessionDeleteResponse;
import com.soulbuddy.domain.chat.dto.response.SessionEndResponse;
import com.soulbuddy.domain.chat.dto.response.SessionItemResponse;
import com.soulbuddy.domain.chat.dto.response.SessionListResponse;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.chat.dto.response.PreChatEmotionResponse;
import com.soulbuddy.domain.chat.dto.response.SessionCreateResponse;
import com.soulbuddy.domain.chat.repository.ChatMessageRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRepository;
import com.soulbuddy.domain.emotion.service.EmotionLogService;
import com.soulbuddy.domain.summary.dto.SummaryCardDto;
import com.soulbuddy.global.enums.*;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiChatService aiChatService;
    private final AiSummaryService aiSummaryService;
    private final EmotionLogService emotionLogService;
    private final com.soulbuddy.domain.summary.service.SummaryService summaryService;

    // POST /api/sessions
    public SessionCreateResponse createSession(Long userId, PersonaType personaType, String nickname) {

        Optional<ChatSession> prevSession = chatSessionRepository
                .findTopByUserIdAndStatusOrderByEndedAtDesc(userId, SessionStatus.ENDED);

        OpeningContext.VisitState visitState = prevSession.isPresent()
                ? OpeningContext.VisitState.RETURNING
                : OpeningContext.VisitState.FIRST;

        String recentSummary = prevSession
                .flatMap(s -> summaryService.findMemoryHintBySessionId(s.getId()))
                .orElse(null);

        ChatSession session = chatSessionRepository.save(
                ChatSession.builder()
                        .userId(userId)
                        .personaType(personaType)
                        .build()
        );

        String openingMsg = aiChatService.openingMessage(
                OpeningContext.builder()
                        .personaType(personaType)
                        .nickname(nickname)
                        .visitState(visitState)
                        .recentSummary(recentSummary)
                        .build()
        );

        chatMessageRepository.save(
                ChatMessage.builder()
                        .sessionId(session.getId())
                        .sender(Sender.ASSISTANT)
                        .content(openingMsg)
                        .aiModel(personaType.aiModelTag())
                        .build()
        );

        return SessionCreateResponse.builder()
                .sessionId(session.getId())
                .personaType(session.getPersonaType())
                .openingMessage(openingMsg)
                .recentSummary(recentSummary)
                .createdAt(session.getCreatedAt())
                .build();
    }
   // GET /api/sessions
    public SessionListResponse getSessions(Long userId, String status, int page, int size) {

        PageRequest pageable = PageRequest.of(page, size);
        Page<ChatSession> sessionPage;

        if (status != null) {
            SessionStatus sessionStatus = SessionStatus.valueOf(status.toUpperCase());
            sessionPage = chatSessionRepository
                    .findByUserIdAndStatusAndDeletedAtIsNullOrderByStartedAtDesc(userId, sessionStatus, pageable);
        } else {
            sessionPage = chatSessionRepository
                    .findByUserIdAndDeletedAtIsNullOrderByStartedAtDesc(userId, pageable);
        }

        List<String> sessionIds = sessionPage.getContent().stream().map(ChatSession::getId).toList();
        Map<String, SummaryCardDto> cardMap = summaryService.findSummaryCardsBySessionIds(sessionIds);

        List<SessionItemResponse> items = sessionPage.getContent().stream()
                .map(s -> {
                    var card = Optional.ofNullable(cardMap.get(s.getId()));
                    return SessionItemResponse.builder()
                            .sessionId(s.getId())
                            .personaType(s.getPersonaType())
                            .characterName(s.getPersonaType().characterName())
                            .status(s.getStatus())
                            .preChatEmotion(s.getPreChatEmotion())
                            .summaryStatus(s.getSummaryStatus())
                            .quoteText(card.map(SummaryCardDto::getQuoteText).orElse(null))
                            .dominantEmotion(card.map(SummaryCardDto::getDominantEmotion).orElse(null))
                            .emotionChange(card.map(SummaryCardDto::getEmotionChange).orElse(null))
                            .startedAt(s.getStartedAt())
                            .endedAt(s.getEndedAt())
                            .build();
                })
                .toList();

        return SessionListResponse.builder()
                .sessions(items)
                .totalCount(sessionPage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }

    // DELETE /api/sessions/{id}
    public SessionDeleteResponse deleteSession(Long userId, String sessionId) {

        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));

        session.softDelete();

        return SessionDeleteResponse.builder()
                .sessionId(session.getId())
                .deletedAt(session.getDeletedAt())
                .build();
    }
    
    // PATCH /api/sessions/{id}/end
    public SessionEndResponse endSession(Long userId, String sessionId) {

        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_002);
        }

        List<ChatMessageDto> messageDtos = chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(m -> ChatMessageDto.builder()
                        .sender(m.getSender())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();

        SummaryResult result = aiSummaryService.summarize(sessionId, userId, messageDtos);

        session.end(SummaryStatus.CREATED);

        return SessionEndResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .summaryStatus(session.getSummaryStatus())
                .summaryText(result.getSummaryText())
                .situationText(result.getSituationText())
                .emotionText(result.getEmotionText())
                .thoughtText(result.getThoughtText())
                .dominantEmotion(result.getDominantEmotion())
                .emotionChange(result.getEmotionChange())
                .memoryHint(result.getMemoryHint())
                .endedAt(session.getEndedAt())
                .build();
    }

    // PATCH /api/sessions/{id}/pre-chat-emotion
    public PreChatEmotionResponse updatePreChatEmotion(Long userId, String sessionId, EmotionTag emotion) {

        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));

        session.updatePreChatEmotion(emotion);

        emotionLogService.log(userId, sessionId, null, emotion, EmotionSource.PRE_CHAT);

        return PreChatEmotionResponse.builder()
                .sessionId(session.getId())
                .preChatEmotion(session.getPreChatEmotion())
                .build();
    }
}
