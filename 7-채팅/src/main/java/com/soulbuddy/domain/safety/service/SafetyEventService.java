package com.soulbuddy.domain.safety.service;

import com.soulbuddy.domain.safety.entity.SafetyEvent;
import com.soulbuddy.global.enums.RiskLevel;

public interface SafetyEventService {
    SafetyEvent recordRiskDetected(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean isForced);
    void recordForcedSafetyReply(Long userId, String sessionId, Long messageId, RiskLevel riskLevel, boolean isForced);
}
