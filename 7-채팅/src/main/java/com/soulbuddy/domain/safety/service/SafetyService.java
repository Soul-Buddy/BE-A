package com.soulbuddy.domain.safety.service;

import com.soulbuddy.domain.safety.dto.SafetyDto;
import com.soulbuddy.domain.safety.entity.SafetyEvent;
import com.soulbuddy.domain.safety.repository.SafetyEventRepository;
import com.soulbuddy.global.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyService implements SafetyEventService {

    private final SafetyEventRepository safetyEventRepository;

    private static final List<String> HIGH_KEYWORDS = List.of(
            "자살", "죽고 싶", "사라지고 싶", "죽어버릴",
            "극단적 선택", "스스로 목숨", "살고 싶지 않", "없어지고 싶"
    );

    private static final List<String> MEDIUM_KEYWORDS = List.of(
            "힘들어", "지쳐", "포기하고 싶", "버티기 힘들",
            "더 이상 못하겠", "살기 싫", "모든 게 끝났으면"
    );

    /**
     * 메인 위험 탐지 로직 (준영 님 내부 사용용)
     */
    @Transactional
    public SafetyDto.RiskDetectResponse detectRisk(SafetyDto.RiskDetectRequest request) {
        RiskLevel finalRiskLevel = determineFinalRiskLevel(
                request.getMessageContent(),
                request.getAiAnalyzedLevel()
        );

        boolean showBanner = shouldShowBanner(finalRiskLevel, request.isForcedSafety());

        if (!showBanner) {
            return SafetyDto.RiskDetectResponse.builder()
                    .showBanner(false)
                    .build();
        }

        // 아래 오버로딩된 메서드를 호출하여 코드 중복 제거
        SafetyEvent detected = recordRiskDetected(
                request.getUserId(), request.getSessionId(), request.getMessageId(),
                finalRiskLevel, request.isForcedSafety()
        );

        recordForcedSafetyReply(
                request.getUserId(), request.getSessionId(), request.getMessageId(),
                finalRiskLevel, request.isForcedSafety()
        );

        return SafetyDto.RiskDetectResponse.builder()
                .showBanner(true)
                .riskLevel(finalRiskLevel)
                .eventId(detected.getId())
                .build();
    }


    @Transactional
    public SafetyEvent recordRiskDetected(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean isForced) {
        log.info("[Safety] Risk Detected: userId={}, level={}", userId, riskLevel);
        return saveEvent(userId, sessionId, messageId, null, SafetyEvent.EventType.RISK_DETECTED, riskLevel, isForced);
    }

    @Transactional
    public void recordForcedSafetyReply(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean isForced) {
        log.info("[Safety] Forced Safety Reply: userId={}, level={}", userId, riskLevel);
        saveEvent(userId, sessionId, messageId, null, SafetyEvent.EventType.BANNER_SHOWN, riskLevel, isForced);
    }

    /**
     * 기존 리소스 관련 이벤트 기록용
     */
    @Transactional
    public SafetyDto.SafetyEventResponse recordEvent(SafetyDto.SafetyEventRequest request) {
        SafetyEvent event = saveEvent(
                request.getUserId(), request.getSessionId(), request.getMessageId(),
                request.getResourceId(), request.getEventType(),
                request.getRiskLevel(), request.isForcedSafety()
        );
        return SafetyDto.SafetyEventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<SafetyDto.SafetyEventResponse> getSessionEvents(String sessionId) {
        return safetyEventRepository.findBySessionIdOrderByCreatedAtDesc(sessionId)
                .stream()
                .map(SafetyDto.SafetyEventResponse::from)
                .toList();
    }

    // ── Private Helpers ───────────────────────────────────────

    private RiskLevel determineFinalRiskLevel(String content, RiskLevel aiLevel) {
        if (content == null || content.isBlank()) return aiLevel != null ? aiLevel : RiskLevel.LOW;
        String lower = content.toLowerCase();

        if (HIGH_KEYWORDS.stream().anyMatch(lower::contains)) return RiskLevel.HIGH;
        if (MEDIUM_KEYWORDS.stream().anyMatch(lower::contains)) return RiskLevel.MEDIUM;

        return aiLevel != null ? aiLevel : RiskLevel.LOW;
    }

    private boolean shouldShowBanner(RiskLevel riskLevel, boolean forcedSafety) {
        return switch (riskLevel) {
            case HIGH, MEDIUM -> true;
            case LOW          -> forcedSafety;
        };
    }

    private SafetyEvent saveEvent(Long userId, String sessionId, Long messageId,
                                  Long resourceId, SafetyEvent.EventType eventType,
                                  RiskLevel riskLevel, boolean forcedSafety) {
        return safetyEventRepository.save(
                SafetyEvent.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .messageId(messageId)
                        .resourceId(resourceId)
                        .eventType(eventType)
                        .riskLevel(riskLevel)
                        .forcedSafety(forcedSafety)
                        .build()
        );
    }
}