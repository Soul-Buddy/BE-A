package com.soulbuddy.domain.safety.repository;

import com.soulbuddy.domain.safety.entity.SafetyEvent;
import com.soulbuddy.global.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SafetyEventRepository extends JpaRepository<SafetyEvent, Long> {

    List<SafetyEvent> findByUserId(Long userId);

    List<SafetyEvent> findBySessionId(String sessionId);

    List<SafetyEvent> findByUserIdAndRiskLevel(Long userId, RiskLevel riskLevel);

    boolean existsBySessionIdAndEventType(String sessionId, SafetyEvent.EventType eventType);

    @Query("SELECT se FROM SafetyEvent se WHERE se.sessionId = :sessionId ORDER BY se.createdAt DESC")
    List<SafetyEvent> findBySessionIdOrderByCreatedAtDesc(@Param("sessionId") String sessionId);
}