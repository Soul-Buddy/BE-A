package com.soulbuddy.domain.dashboard.service;

import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.dashboard.dto.DashboardResponse;
import com.soulbuddy.domain.emotion.entity.EmotionLog;
import com.soulbuddy.domain.emotion.repository.EmotionLogRepository;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.domain.summary.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EmotionLogRepository emotionLogRepository;
    private final SummaryRepository summaryRepository;

    public DashboardResponse getDashboard(Long userId, Pageable pageable) {
        // 감정 통계 집계
        List<EmotionLog> logs = emotionLogRepository.findByUserId(userId);
        Map<String, Long> emotionStats = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getEmotionTag().name(),
                        Collectors.counting()
                ));

        // 최근 요약 카드 (페이징)
        Page<Summary> summaryPage = summaryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<DashboardResponse.SummaryCard> summaryCards = summaryPage.getContent().stream()
                .map(this::toSummaryCard)
                .toList();

        return DashboardResponse.builder()
                .userId(userId)
                .emotionStats(emotionStats)
                .recentSummaries(summaryCards)
                .totalSummaryCount(summaryPage.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    private DashboardResponse.SummaryCard toSummaryCard(Summary summary) {
        ChatSession session = summary.getSession();

        return DashboardResponse.SummaryCard.builder()
                .sessionId(session.getId())
                .date(summary.getCreatedAt().toLocalDate().toString())
                .summary(summary.getSummaryText())
                .dominantEmotion(summary.getDominantEmotion() != null
                        ? summary.getDominantEmotion().name() : null)
                .personaType(session.getPersonaType().name())
                .build();
    }
}
