package com.soulbuddy.domain.summary.service;

import com.soulbuddy.domain.summary.dto.SummaryCardDto;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.domain.summary.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SummaryServiceImpl implements SummaryService {

    private final SummaryRepository summaryRepository;

    @Override
    public Optional<String> findMemoryHintBySessionId(String sessionId) {
        return summaryRepository.findBySessionId(sessionId)
                .map(Summary::getMemoryHint);
    }

    @Override
    public Optional<SummaryCardDto> findSummaryCardBySessionId(String sessionId) {
        return summaryRepository.findBySessionId(sessionId)
                .map(s -> SummaryCardDto.builder()
                        .quoteText(s.getQuoteText())
                        .dominantEmotion(s.getDominantEmotion())
                        .emotionChange(s.getEmotionChange())
                        .build());
    }

    @Override
    public Map<String, SummaryCardDto> findSummaryCardsBySessionIds(List<String> sessionIds) {
        return summaryRepository.findAllBySessionIdIn(sessionIds).stream()
                .collect(Collectors.toMap(
                        Summary::getSessionId,
                        s -> SummaryCardDto.builder()
                                .quoteText(s.getQuoteText())
                                .dominantEmotion(s.getDominantEmotion())
                                .emotionChange(s.getEmotionChange())
                                .build()
                ));
    }

    @Override
    public Optional<Summary> findBySessionId(String sessionId) {
        return summaryRepository.findBySessionId(sessionId);
    }
}
