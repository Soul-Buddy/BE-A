package com.soulbuddy.domain.summary.service;

import com.soulbuddy.domain.summary.dto.SummaryCardDto;
import com.soulbuddy.domain.summary.entity.Summary;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SummaryService {

    Optional<String> findMemoryHintBySessionId(String sessionId);

    Optional<SummaryCardDto> findSummaryCardBySessionId(String sessionId);

    Map<String, SummaryCardDto> findSummaryCardsBySessionIds(List<String> sessionIds);

    Optional<Summary> findBySessionId(String sessionId);
}
