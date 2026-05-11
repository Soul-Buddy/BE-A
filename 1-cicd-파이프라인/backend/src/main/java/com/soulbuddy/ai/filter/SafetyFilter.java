package com.soulbuddy.ai.filter;

import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.global.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SafetyFilter {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of(
            "자살", "자해", "죽고 싶", "죽고싶", "사라지고 싶", "사라지고싶",
            "없어지고 싶", "없어지고싶", "극단적", "목숨", "유서",
            "스스로 목숨", "살기 싫", "살기싫", "죽는 게 낫", "죽는게 낫"
    );

    public static final String SAFETY_MESSAGE =
            "지금 많이 힘드시겠어요. 혼자 감당하기 어려운 순간엔 " +
            "전문가와 이야기 나눠보는 것이 큰 도움이 될 수 있어요. " +
            "정신건강 위기상담 전화 1577-0199는 24시간 운영됩니다. " +
            "언제든 연락해보실 수 있어요.";

    public static final String SAFETY_ACTION =
            "정신건강 위기상담 전화 1577-0199 (24시간 운영)";

    public static final String MEDIUM_RECOMMEND =
            "\n\n요즘 많이 힘드신 것 같아서, 전문 상담사와 이야기 나눠보시는 것도 " +
            "좋은 방법일 수 있어요. 혼자 감당하지 않아도 괜찮아요.";

    public RiskLevel preCheck(String userMessage) {
        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (userMessage.contains(keyword)) {
                return RiskLevel.HIGH;
            }
        }
        return RiskLevel.LOW;
    }

    public ChatResponse postProcess(ChatResponse response) {
        if ("HIGH".equals(response.getRiskLevel())) {
            return ChatResponse.builder()
                    .assistantMessage(SAFETY_MESSAGE)
                    .emotionTag(response.getEmotionTag())
                    .riskLevel("HIGH")
                    .memoryHint(response.getMemoryHint())
                    .recommendedAction(SAFETY_ACTION)
                    .build();
        }
        if ("MEDIUM".equals(response.getRiskLevel())) {
            return ChatResponse.builder()
                    .assistantMessage(response.getAssistantMessage() + MEDIUM_RECOMMEND)
                    .emotionTag(response.getEmotionTag())
                    .riskLevel("MEDIUM")
                    .memoryHint(response.getMemoryHint())
                    .recommendedAction(response.getRecommendedAction())
                    .build();
        }
        return response;
    }

    public ChatResponse buildSafetyResponse() {
        return ChatResponse.builder()
                .assistantMessage(SAFETY_MESSAGE)
                .emotionTag("ANXIOUS")
                .riskLevel("HIGH")
                .memoryHint(null)
                .recommendedAction(SAFETY_ACTION)
                .build();
    }
}
