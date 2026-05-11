package com.soulbuddy.ai.client;

import com.soulbuddy.global.config.ClovaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * HCX-007 (베이스) 세션 요약 호출.
 * 응답 형식: JSON {summaryText, situationText, emotionText, thoughtText,
 *                 dominantEmotion, emotionDistribution, emotionChange,
 *                 quoteText, memoryHint}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryLlmClient {

    private static final int MAX_TOKENS = 1200;
    private static final double TEMPERATURE = 0.3;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            당신은 정서 지원 대화 세션을 요약하는 AI입니다.
            아래 사용자-어시스턴트 대화 전체를 읽고, 반드시 JSON 형식으로만 응답하세요.

            출력 JSON 스키마:
            {
              "summaryText":   "세션 전체를 한국어 4~6문장으로 요약",
              "situationText": "겉으로 드러난 객관적 상황 1~3문장",
              "emotionText":   "사용자가 표현한 감정과 변화 1~3문장",
              "thoughtText":   "사용자가 보여준 생각·신념·해석 1~3문장",
              "dominantEmotion": "HAPPY|SAD|ANGRY|ANXIOUS|HURT|EMBARRASSED 중 하나",
              "emotionDistribution": {"HAPPY":0,"SAD":0,"ANGRY":0,"ANXIOUS":0,"HURT":0,"EMBARRASSED":0},
              "emotionChange": "예: '불안 → 다소 안정' (한국어)",
              "quoteText":     "기록 카드에 쓸 짧은 인용구 한 문장 (50자 이내)",
              "memoryHint":    "다음 세션에 주입할 200자 이내 압축 메모. 형식: '[사실] ... [감정] ...'"
            }

            규칙:
            - JSON 외 다른 텍스트 절대 출력 금지.
            - 진단·처방·병명 단정 금지.
            - emotionDistribution 합계는 100 이하.
            """;

    private final ClovaHttpClient clovaHttpClient;
    private final ClovaProperties clovaProperties;

    public String summarize(String fullDialogText) {
        return clovaHttpClient.callJson(
                clovaProperties.getEndpoint().getSummary(),
                clovaProperties.getRequestId().getSummary(),
                clovaHttpClient.buildBody(List.of(
                        Map.of("role", "system", "content", SUMMARY_SYSTEM_PROMPT),
                        Map.of("role", "user", "content", fullDialogText)
                ), TEMPERATURE, MAX_TOKENS));
    }
}
