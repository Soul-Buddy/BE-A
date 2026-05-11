package com.soulbuddy.domain.chat.service;

import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.ai.service.AiSummaryService;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.chat.repository.ChatMessageRepository;
import com.soulbuddy.domain.chat.repository.ChatSessionRepository;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.domain.summary.repository.SummaryRepository;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.repository.UserRepository;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.enums.Sender;
import com.soulbuddy.global.enums.SessionStatus;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SummaryRepository summaryRepository;
    private final UserRepository userRepository;
    private final AiSummaryService aiSummaryService;

    private static final String OPENING_MESSAGE_FIRST = "안녕하세요. 저는 말벗 오누이입니다. 오늘은 어떤 일로 고민이신가요?";
    private static final String OPENING_MESSAGE_RETURN = "오늘은 어떤 일이 있었나요?";
    private static final int MAX_RECENT_SUMMARIES = 7;
    private static final int RECENT_SUMMARY_MAX_LENGTH = 500;

    @Transactional
    public ChatSession createSession(Long userId, PersonaType personaType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        ChatSession session = ChatSession.builder()
                .user(user)
                .personaType(personaType)
                .build();
        chatSessionRepository.save(session);

        // 오프닝 메시지 저장
        boolean hasPreviousSession = chatSessionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(2))
                .getTotalElements() > 1;

        String openingMessage = hasPreviousSession ? OPENING_MESSAGE_RETURN : OPENING_MESSAGE_FIRST;

        ChatMessage opening = ChatMessage.builder()
                .session(session)
                .sender(Sender.ASSISTANT)
                .content(openingMessage)
                .build();
        chatMessageRepository.save(opening);

        return session;
    }

    public Page<ChatSession> getSessions(Long userId, Pageable pageable) {
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public ChatSession getSession(String sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));
    }

    @Transactional
    public Summary endSession(Long userId, String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_001));

        // 세션 종료 처리
        ChatSession ended = ChatSession.builder()
                .id(session.getId())
                .user(session.getUser())
                .personaType(session.getPersonaType())
                .status(SessionStatus.ENDED)
                .title(session.getTitle())
                .startedAt(session.getStartedAt())
                .endedAt(LocalDateTime.now())
                .build();
        chatSessionRepository.save(ended);

        // 전체 대화 가져와서 요약 생성
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        SummaryResult result = aiSummaryService.summarize(messages);

        // 감정 변화 흐름 계산
        String emotionChange = buildEmotionChange(messages);

        // Summary 저장
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        Summary summary = Summary.builder()
                .session(session)
                .user(user)
                .summaryText(result.getSummaryText())
                .dominantEmotion(parseEmotionTag(result.getDominantEmotion()))
                .emotionChange(emotionChange)
                .memoryHint(result.getMemoryHint())
                .build();

        return summaryRepository.save(summary);
    }

    /**
     * 최근 종료 세션들의 요약을 압축하여 recentSummary 문자열을 생성합니다.
     * 최대 7개 세션, 500자 제한.
     */
    public String buildRecentSummary(Long userId) {
        Page<Summary> recentPage = summaryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(MAX_RECENT_SUMMARIES));

        List<Summary> summaries = recentPage.getContent();
        if (summaries.isEmpty()) {
            return null;
        }

        // 오래된 순으로 정렬 (역순)
        List<Summary> oldest = summaries.reversed();

        StringBuilder sb = new StringBuilder("[과거 대화 요약]\n");
        int totalSessions = oldest.size();

        for (int i = 0; i < totalSessions; i++) {
            Summary s = oldest.get(i);
            String label = getSessionLabel(totalSessions - i);

            String line = String.format("(%s) 상황: %s / 핵심감정: %s / 감정변화: %s\n",
                    label,
                    s.getSummaryText(),
                    s.getDominantEmotion() != null ? s.getDominantEmotion().name() : "NEUTRAL",
                    s.getEmotionChange() != null ? s.getEmotionChange() : "없음");

            if (sb.length() + line.length() > RECENT_SUMMARY_MAX_LENGTH) {
                break;
            }
            sb.append(line);
        }

        return sb.toString().trim();
    }

    private String getSessionLabel(int sessionsAgo) {
        if (sessionsAgo == 1) return "지난 대화";
        return sessionsAgo + "회 전";
    }

    private String buildEmotionChange(List<ChatMessage> messages) {
        List<String> emotions = messages.stream()
                .filter(m -> m.getSender() == Sender.ASSISTANT && m.getEmotionTag() != null)
                .map(m -> m.getEmotionTag().name())
                .toList();

        if (emotions.isEmpty()) return null;
        if (emotions.size() == 1) return emotions.get(0);

        return emotions.getFirst() + " → " + emotions.getLast();
    }

    private EmotionTag parseEmotionTag(String value) {
        try {
            return EmotionTag.valueOf(value);
        } catch (Exception e) {
            return EmotionTag.NEUTRAL;
        }
    }
}
