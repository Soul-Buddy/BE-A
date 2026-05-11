package com.soulbuddy.ai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.SummaryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiResponseParser {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MEMORY_HINT_MAX_LENGTH = 200;

    public ChatResponse parse(String rawJson) {
        try {
            JsonNode node = mapper.readTree(rawJson);

            String memoryHint = getTextOrNull(node, "memoryHint");
            if (memoryHint != null && memoryHint.length() > MEMORY_HINT_MAX_LENGTH) {
                memoryHint = memoryHint.substring(0, MEMORY_HINT_MAX_LENGTH) + "...";
            }

            return ChatResponse.builder()
                    .assistantMessage(node.get("assistantMessage").asText())
                    .emotionTag(node.get("emotionTag").asText())
                    .riskLevel(node.get("riskLevel").asText())
                    .memoryHint(memoryHint)
                    .summary(null)
                    .recommendedAction(null)
                    .build();
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", e.getMessage());
            return fallback();
        }
    }

    public SummaryResult parseSummary(String rawJson) {
        try {
            JsonNode node = mapper.readTree(rawJson);

            String memoryHint = getTextOrNull(node, "memoryHint");
            if (memoryHint != null && memoryHint.length() > MEMORY_HINT_MAX_LENGTH) {
                memoryHint = memoryHint.substring(0, MEMORY_HINT_MAX_LENGTH) + "...";
            }

            return SummaryResult.builder()
                    .summaryText(node.get("summaryText").asText())
                    .dominantEmotion(node.get("dominantEmotion").asText())
                    .memoryHint(memoryHint)
                    .build();
        } catch (Exception e) {
            log.error("요약 응답 파싱 실패: {}", e.getMessage());
            return SummaryResult.builder()
                    .summaryText("요약 생성에 실패했습니다.")
                    .dominantEmotion("ANXIOUS")
                    .memoryHint(null)
                    .build();
        }
    }

    public ChatResponse fallback() {
        return ChatResponse.builder()
                .assistantMessage("죄송해요, 지금 잠시 연결이 불안정해요. 잠깐 후에 다시 이야기해요.")
                .emotionTag("ANXIOUS")
                .riskLevel("LOW")
                .memoryHint(null)
                .recommendedAction(null)
                .build();
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || "null".equals(value.asText())) {
            return null;
        }
        return value.asText();
    }
}
