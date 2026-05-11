package com.soulbuddy.domain.safety.entity;

import com.soulbuddy.global.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "safety_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SafetyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // users 테이블 FK (CASCADE DELETE)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // chat_sessions 테이블 FK (CASCADE DELETE)
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    // chat_messages 테이블 FK (SET NULL)
    @Column(name = "message_id")
    private Long messageId;

    // counseling_centers 테이블 FK (SET NULL)
    @Column(name = "resource_id")
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    // MEDIUM / HIGH 만 존재 (LOW는 오버레이 미표시라 저장 안 함)
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "forced_safety", nullable = false)
    private boolean forcedSafety;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum EventType {
        RISK_DETECTED,          // 위험 감지 (AI 분석 결과)
        BANNER_SHOWN,           // Safety Check 배너 표시됨
        ASSESSMENT_OPENED,      // 자가진단 열기
        ASSESSMENT_COMPLETED,   // 자가진단 완료
        CENTER_LIST_VIEWED,     // 근처 상담센터 목록 조회
        CENTER_CALL_TAPPED,     // 상담센터 전화 버튼 탭 (109, 1388, 1577-0199)
        FORCED_SAFETY_REPLY     // 강제 안전 답변 전송
    }
}