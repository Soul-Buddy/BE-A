package com.soulbuddy.domain.dashboard.service;

import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.chat.repository.ChatSessionRepository;
import com.soulbuddy.domain.dashboard.dto.response.DashboardResponse;
import com.soulbuddy.domain.emotion.repository.EmotionLogRepository;
import com.soulbuddy.domain.summary.dto.SummaryCardDto;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.SessionStatus;
import com.soulbuddy.global.enums.SummaryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ChatSessionRepository chatSessionRepository;
    private final EmotionLogRepository emotionLogRepository;
    private final com.soulbuddy.domain.summary.service.SummaryService summaryService;

    // GET /api/dashboard/me
    public DashboardResponse getDashboard(Long userId, int page, int size) {

        // 감정 통계
        Map<EmotionTag, Long> emotionStats = Arrays.stream(EmotionTag.values())
                .collect(Collectors.toMap(tag -> tag, tag -> 0L));

        emotionLogRepository.countEmotionTagByUserId(userId)
                .forEach(row -> emotionStats.put(row.getEmotionTag(), row.getCount()));

        // 요약 카드 목록
        Page<ChatSession> sessionPage = chatSessionRepository
                .findByUserIdAndStatusAndSummaryStatusAndDeletedAtIsNullOrderByEndedAtDesc(
                        userId, SessionStatus.ENDED, SummaryStatus.CREATED, PageRequest.of(page, size));

        List<String> sessionIds = sessionPage.getContent().stream().map(ChatSession::getId).toList();
        Map<String, SummaryCardDto> cardMap = summaryService.findSummaryCardsBySessionIds(sessionIds);

        List<DashboardResponse.SummaryCard> summaryCards = sessionPage.getContent().stream()
                .map(s -> {
                    var card = Optional.ofNullable(cardMap.get(s.getId()));
                    return DashboardResponse.SummaryCard.builder()
                            .sessionId(s.getId())
                            .date(s.getEndedAt().toLocalDate())
                            .personaType(s.getPersonaType())
                            .characterName(s.getPersonaType().characterName())
                            .quoteText(card.map(SummaryCardDto::getQuoteText).orElse(null))
                            .dominantEmotion(card.map(SummaryCardDto::getDominantEmotion).orElse(null))
                            .emotionChange(card.map(SummaryCardDto::getEmotionChange).orElse(null))
                            .build();
                })
                .toList();

        return DashboardResponse.builder()
                .userId(userId)
                .emotionStats(emotionStats)
                .recentSummaries(summaryCards)
                .totalSummaryCount(sessionPage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }
}
