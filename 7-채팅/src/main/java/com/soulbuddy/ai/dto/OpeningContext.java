package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.enums.RiskLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 세션 오프닝 메시지 생성 호출 시 입력 컨텍스트.
 * production: 헌영 SessionService가 직전 종료 세션·Profile을 조회해 자체 조립.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpeningContext {

    @NotNull
    private PersonaType personaType;

    private String nickname;

    @NotNull
    private VisitState visitState;

    private String recentSummary;

    private String priorTopic;

    private RiskLevel priorSeverity;

    private boolean priorUnresolved;

    public enum VisitState {
        FIRST,
        RETURNING
    }
}
