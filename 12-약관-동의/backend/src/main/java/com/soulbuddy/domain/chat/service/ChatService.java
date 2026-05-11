package com.soulbuddy.domain.chat.service;

import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.ai.filter.SafetyFilter;
import com.soulbuddy.ai.service.AiChatService;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.chat.repository.ChatMessageRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRepository;
import com.soulbuddy.domain.emotion.service.EmotionLogService;
import com.soulbuddy.domain.user.entity.Profile;
import com.soulbuddy.domain.user.repository.ProfileRepository;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.RiskLevel;
import com.soulbuddy.global.enums.Sender;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProfileRepository profileRepository;
    private final AiChatService aiChatService;
    private final SafetyFilter safetyFilter;
    private final EmotionLogService emotionLogService;

    private static final int MAX_HISTORY_SIZE = 10;
    private static final int PERSONALITY_MAX_LENGTH = 100;
    private static final int HOBBIES_MAX_COUNT = 3;
    private static final int CONCERNS_MAX_COUNT = 3;
    private static final int RECENT_SUMMARY_MAX_LENGTH = 200;

    @Transactional
    public ChatResponse processChat(Long userId, ChatRequest request) {
        ChatSession session = chatSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));

        // 1. 기존 히스토리 조회 (현재 메시지 저장 전)
        List<ChatMessage> allMessages = chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(request.getSessionId());
        int fromIndex = Math.max(0, allMessages.size() - MAX_HISTORY_SIZE);
        List<ChatMessage> recentHistory = allMessages.subList(fromIndex, allMessages.size());

        // 2. 유저 메시지 DB 저장
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .sender(Sender.USER)
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMsg);

        // 3. SafetyFilter 1차 검사 (룰 기반)
        RiskLevel preCheckResult = safetyFilter.preCheck(request.getMessage());
        if (preCheckResult == RiskLevel.HIGH) {
            ChatResponse safetyResponse = safetyFilter.buildSafetyResponse();
            saveAssistantMessage(session, safetyResponse);
            emotionLogService.save(userId, session, EmotionTag.ANXIOUS);
            return safetyResponse;
        }

        // 4. PromptContext 조립
        PromptContext context = buildPromptContext(userId, request);

        // 5. AI 파이프라인 호출 (히스토리 + 현재 메시지 포함)
        recentHistory.add(userMsg);
        ChatResponse response = aiChatService.process(context, recentHistory);

        // 6. AI 응답 메시지 DB 저장
        saveAssistantMessage(session, response);

        // 7. EmotionLog 저장
        EmotionTag emotionTag = parseEmotionTag(response.getEmotionTag());
        emotionLogService.save(userId, session, emotionTag);

        return response;
    }

    public Page<ChatMessage> getChatHistory(String sessionId, Pageable pageable) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, pageable);
    }

    private PromptContext buildPromptContext(Long userId, ChatRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));

        // Truncate 규칙 적용
        String personality = truncate(profile.getPersonality(), PERSONALITY_MAX_LENGTH);
        List<String> hobbies = limitList(profile.getHobbies(), HOBBIES_MAX_COUNT);
        List<String> concerns = limitList(profile.getConcerns(), CONCERNS_MAX_COUNT);
        String recentSummary = truncate(request.getRecentSummary(), RECENT_SUMMARY_MAX_LENGTH);

        return PromptContext.builder()
                .nickname(profile.getNickname())
                .preferredTone(profile.getPreferredTone())
                .personality(personality)
                .hobbies(hobbies)
                .concerns(concerns)
                .recentSummary(recentSummary)
                .personaType(request.getPersonaType())
                .build();
    }

    private void saveAssistantMessage(ChatSession session, ChatResponse response) {
        ChatMessage aiMsg = ChatMessage.builder()
                .session(session)
                .sender(Sender.ASSISTANT)
                .content(response.getAssistantMessage())
                .emotionTag(parseEmotionTag(response.getEmotionTag()))
                .riskLevel(parseRiskLevel(response.getRiskLevel()))
                .build();
        chatMessageRepository.save(aiMsg);
    }

    private EmotionTag parseEmotionTag(String value) {
        try {
            return EmotionTag.valueOf(value);
        } catch (Exception e) {
            return EmotionTag.ANXIOUS;
        }
    }

    private RiskLevel parseRiskLevel(String value) {
        try {
            return RiskLevel.valueOf(value);
        } catch (Exception e) {
            return RiskLevel.LOW;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }

    private List<String> limitList(List<String> list, int maxCount) {
        if (list == null || list.size() <= maxCount) return list;
        return list.subList(0, maxCount);
    }
}
