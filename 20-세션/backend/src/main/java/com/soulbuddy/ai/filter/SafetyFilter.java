package com.soulbuddy.ai.filter;

import com.soulbuddy.global.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 위험도 1차 룰 기반 필터 + Safety Gate.
 * - preCheck: 사용자 입력에서 HIGH 키워드 즉시 감지 (LLM 호출 스킵 가능)
 * - decideForcedSafety: 분류 결과 + 누적 HIGH 횟수로 강제 안전 발화 결정
 *
 * 강제 안전 발화 본문/추천 액션은 system_prompts_final.txt 공통 규칙 5번을 따름.
 */
@Component
public class SafetyFilter {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of(
            "자살", "자해", "죽고 싶", "죽고싶", "사라지고 싶", "사라지고싶",
            "없어지고 싶", "없어지고싶", "극단적", "목숨", "유서",
            "스스로 목숨", "살기 싫", "살기싫", "죽는 게 낫", "죽는게 낫"
    );

    public static final String SAFETY_MESSAGE =
            "지금 많이 힘드시겠어요. 혼자 감당하기 어려운 순간엔 전문가와 이야기 나눠보는 것이 " +
            "큰 도움이 될 수 있어요. 자살예방 상담전화 109(24시간), 청소년상담 1388, " +
            "정신건강 위기상담 1577-0199로 언제든 연락하실 수 있어요.";

    public static final String SAFETY_ACTION =
            "자살예방 상담전화 109 (24시간 운영)";

    public static final String MEDIUM_RECOMMEND =
            "\n\n요즘 마음이 많이 무거우신 것 같아요. 필요하시면 전문 상담사와 짧게라도 " +
            "이야기 나눠보시는 것도 도움이 될 수 있어요. 혼자 견디지 않으셔도 됩니다.";

    /** 사용자 입력에 명시적 HIGH 표현이 있으면 LLM 호출 전에 차단. */
    public boolean isImmediateHighRisk(String userMessage) {
        if (userMessage == null) return false;
        String s = userMessage.toLowerCase();
        for (String kw : HIGH_RISK_KEYWORDS) {
            if (s.contains(kw)) return true;
        }
        return false;
    }

    /**
     * 강제 안전 발화로 일반 응답을 차단할지 결정.
     * - 즉시 키워드 감지 → true
     * - 분류기 HIGH + 최근 HIGH 누적 횟수 ≥ threshold → true
     */
    public boolean decideForcedSafety(boolean immediateHigh, RiskLevel classifiedRisk,
                                      long recentHighCount, int threshold) {
        if (immediateHigh) return true;
        if (classifiedRisk == RiskLevel.HIGH && recentHighCount + 1 >= threshold) return true;
        return false;
    }
}
