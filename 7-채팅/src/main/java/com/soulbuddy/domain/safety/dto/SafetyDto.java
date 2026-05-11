package com.soulbuddy.domain.safety.dto;

import com.soulbuddy.domain.safety.entity.SafetyEvent;
import com.soulbuddy.global.enums.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class SafetyDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "메시지 위험도 분석 요청")
    public static class RiskDetectRequest {

        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "채팅 세션 ID (UUID 36자)", example = "550e8400-e29b-41d4-a716-446655440000")
        private String sessionId;

        @Schema(description = "분석 대상 메시지 ID", example = "42")
        private Long messageId;

        @Schema(description = "분석할 메시지 내용", example = "요즘 너무 힘들어서 포기하고 싶어")
        private String messageContent;

        @Schema(description = "강제 안전 모드 여부", example = "false")
        private boolean forcedSafety;

        // 👇 추가된 필드: AI 분석 결과를 프론트나 AI 서버로부터 전달받음
        @Schema(description = "AI가 분석한 위험 수준 (LOW, MEDIUM, HIGH)", example = "HIGH")
        private RiskLevel aiAnalyzedLevel;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "사용자 액션 이벤트 기록 요청")
    public static class SafetyEventRequest {

        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "채팅 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private String sessionId;

        @Schema(description = "연관 메시지 ID", example = "42")
        private Long messageId;

        @Schema(description = "상담센터 ID (CENTER_CALL_TAPPED, CENTER_LIST_VIEWED 시)", example = "1")
        private Long resourceId;

        @Schema(
                description = """
                        이벤트 타입
                        - BANNER_SHOWN: Safety Check 배너 표시됨
                        - ASSESSMENT_OPENED: 자가진단 열기
                        - ASSESSMENT_COMPLETED: 자가진단 완료
                        - CENTER_LIST_VIEWED: 근처 상담센터 목록 조회
                        - CENTER_CALL_TAPPED: 상담센터 전화 버튼 탭 (109, 1388, 1577-0199)
                        - FORCED_SAFETY_REPLY: 강제 안전 답변 전송
                        """,
                example = "CENTER_CALL_TAPPED"
        )
        private SafetyEvent.EventType eventType;

        @Schema(description = "위험 수준 (MEDIUM / HIGH)", example = "HIGH")
        private RiskLevel riskLevel;

        @Schema(description = "강제 안전 모드 여부", example = "false")
        private boolean forcedSafety;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "위험도 분석 결과")
    public static class RiskDetectResponse {

        @Schema(description = "배너 표시 여부. true면 Safety Check 배너를 표시해야 함", example = "true")
        private boolean showBanner;

        @Schema(description = "위험 수준 (MEDIUM / HIGH). LOW는 배너 미표시라 반환 안 함", example = "HIGH")
        private RiskLevel riskLevel;

        @Schema(description = "생성된 safety_events ID", example = "101")
        private Long eventId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "이벤트 단건 응답")
    public static class SafetyEventResponse {

        @Schema(description = "이벤트 ID", example = "202")
        private Long id;

        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private String sessionId;

        @Schema(description = "이벤트 타입", example = "CENTER_CALL_TAPPED")
        private SafetyEvent.EventType eventType;

        @Schema(description = "위험 수준", example = "HIGH")
        private RiskLevel riskLevel;

        @Schema(description = "강제 안전 모드 여부", example = "false")
        private boolean forcedSafety;

        @Schema(description = "생성 시각", example = "2025-05-09T09:41:00")
        private String createdAt;

        public static SafetyEventResponse from(SafetyEvent event) {
            return SafetyEventResponse.builder()
                    .id(event.getId())
                    .userId(event.getUserId())
                    .sessionId(event.getSessionId())
                    .eventType(event.getEventType())
                    .riskLevel(event.getRiskLevel())
                    .forcedSafety(event.isForcedSafety())
                    .createdAt(event.getCreatedAt().toString())
                    .build();
        }
    }
}