package com.soulbuddy.ai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.ai.filter.SafetyFilter;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CLOVA 페르소나 응답은 자연어 텍스트만 반환하므로 JSON 파싱이 아닌 통과(passthrough)이며,
 * memoryHint truncate / fallback 메시지 / HCX-007 요약 JSON 파싱을 담당합니다.
 */
@Slf4j
@Component
public class AiResponseParser {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MEMORY_HINT_MAX_LENGTH = 200;

    /** 페르소나 응답 후처리: 빈 응답이면 fallback. */
    public String sanitizeAssistantMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return fallbackAssistantMessage();
        }
        return raw.trim();
    }

    public String truncateMemoryHint(String memoryHint) {
        if (memoryHint == null) return null;
        if (memoryHint.length() <= MEMORY_HINT_MAX_LENGTH) return memoryHint;
        return memoryHint.substring(0, MEMORY_HINT_MAX_LENGTH) + "...";
    }

    public ChatResponse fallback() {
        return ChatResponse.builder()
                .assistantMessage(fallbackAssistantMessage())
                .emotionTag(EmotionTag.ANXIOUS)
                .riskLevel(RiskLevel.LOW)
                .interventionType(null)
                .ragUsed(false)
                .aiModel(null)
                .forcedSafety(false)
                .summary(null)
                .memoryHint(null)
                .recommendedAction(null)
                .build();
    }

    public String fallbackAssistantMessage() {
        return "죄송해요, 지금 잠시 연결이 불안정해요. 잠깐 후에 다시 이야기해요.";
    }

    public ChatResponse safetyResponse() {
        return ChatResponse.builder()
                .assistantMessage(SafetyFilter.SAFETY_MESSAGE)
                .emotionTag(EmotionTag.HURT)
                .riskLevel(RiskLevel.HIGH)
                .interventionType(null)
                .ragUsed(false)
                .aiModel("SYSTEM")
                .forcedSafety(true)
                .summary(null)
                .memoryHint(null)
                .recommendedAction(SafetyFilter.SAFETY_ACTION)
                .build();
    }

    /** HCX-007 요약 응답 (JSON 형식 강제) → SummaryResult. */
    public SummaryResult parseSummary(String rawJson) {
        try {
            JsonNode node = mapper.readTree(rawJson);
            String memoryHint = truncateMemoryHint(getTextOrNull(node, "memoryHint"));
            EmotionTag dominant = parseEmotion(getTextOrNull(node, "dominantEmotion"));

            Map<String, Integer> distribution = new LinkedHashMap<>();
            JsonNode dist = node.get("emotionDistribution");
            if (dist != null && dist.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = dist.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    distribution.put(e.getKey(), e.getValue().asInt(0));
                }
            }

            return SummaryResult.builder()
                    .summaryText(getTextOrNull(node, "summaryText"))
                    .situationText(getTextOrNull(node, "situationText"))
                    .emotionText(getTextOrNull(node, "emotionText"))
                    .thoughtText(getTextOrNull(node, "thoughtText"))
                    .dominantEmotion(dominant)
                    .emotionDistribution(distribution)
                    .emotionChange(getTextOrNull(node, "emotionChange"))
                    .quoteText(getTextOrNull(node, "quoteText"))
                    .memoryHint(memoryHint)
                    .build();
        } catch (Exception e) {
            log.error("HCX-007 요약 응답 파싱 실패: {}", e.getMessage());
            return SummaryResult.builder()
                    .summaryText("요약 생성에 실패했습니다.")
                    .dominantEmotion(null)
                    .emotionDistribution(new HashMap<>())
                    .memoryHint(null)
                    .build();
        }
    }

    private static EmotionTag parseEmotion(String s) {
        if (s == null) return null;
        return EmotionTag.fromJson(s);
    }

    private static String getTextOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || "null".equals(value.asText())) return null;
        return value.asText();
    }
}
