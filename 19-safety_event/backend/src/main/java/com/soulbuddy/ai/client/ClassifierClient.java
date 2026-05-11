package com.soulbuddy.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulbuddy.ai.dto.ClassificationResult;
import com.soulbuddy.ai.prompt.SystemPromptLoader;
import com.soulbuddy.global.config.ClovaProperties;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.InterventionType;
import com.soulbuddy.global.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * DASH-002 ft 3종 병렬 분류 호출:
 *   A. emotion (한국어 라벨 → EmotionTag)
 *   B. risk    (JSON {"risk":"LOW|MEDIUM|HIGH",...})
 *   C. intervention (영문 snake_case 코드)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassifierClient {

    private static final int MAX_TOKENS = 100;
    private static final double TEMPERATURE = 0.0;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Executor PARALLEL_EXECUTOR = Executors.newFixedThreadPool(6);

    private final ClovaHttpClient clovaHttpClient;
    private final ClovaProperties clovaProperties;
    private final SystemPromptLoader promptLoader;

    public ClassificationResult classifyParallel(String userMessage) {
        CompletableFuture<EmotionTag> emo = CompletableFuture.supplyAsync(
                () -> classifyEmotion(userMessage), PARALLEL_EXECUTOR);
        CompletableFuture<RiskLevel> risk = CompletableFuture.supplyAsync(
                () -> classifyRisk(userMessage), PARALLEL_EXECUTOR);
        CompletableFuture<InterventionType> iv = CompletableFuture.supplyAsync(
                () -> classifyIntervention(userMessage), PARALLEL_EXECUTOR);

        try {
            CompletableFuture.allOf(emo, risk, iv).get();
            return ClassificationResult.builder()
                    .emotion(emo.get())
                    .risk(risk.get())
                    .intervention(iv.get())
                    .build();
        } catch (Exception e) {
            log.error("분류기 병렬 호출 실패: {}", e.getMessage());
            return ClassificationResult.builder()
                    .emotion(EmotionTag.ANXIOUS)
                    .risk(RiskLevel.LOW)
                    .intervention(InterventionType.SYMPATHY_SUPPORT)
                    .build();
        }
    }

    public EmotionTag classifyEmotion(String userMessage) {
        String raw = clovaHttpClient.callJson(
                clovaProperties.getEndpoint().getClassifyEmotion(),
                clovaProperties.getRequestId().getClassifyEmotion(),
                clovaHttpClient.buildBody(List.of(
                        Map.of("role", "system", "content", promptLoader.getClassifierEmotion()),
                        Map.of("role", "user", "content", userMessage)
                ), TEMPERATURE, MAX_TOKENS));
        if (raw == null) return EmotionTag.ANXIOUS;
        EmotionTag tag = EmotionTag.fromJson(raw.trim());
        return tag != null ? tag : EmotionTag.ANXIOUS;
    }

    public RiskLevel classifyRisk(String userMessage) {
        String raw = clovaHttpClient.callJson(
                clovaProperties.getEndpoint().getClassifyRisk(),
                clovaProperties.getRequestId().getClassifyRisk(),
                clovaHttpClient.buildBody(List.of(
                        Map.of("role", "system", "content", promptLoader.getClassifierRisk()),
                        Map.of("role", "user", "content", userMessage)
                ), TEMPERATURE, MAX_TOKENS));
        if (raw == null) return RiskLevel.LOW;
        try {
            JsonNode node = mapper.readTree(raw.trim());
            JsonNode r = node.get("risk");
            if (r != null) {
                String v = r.asText("LOW").trim().toUpperCase();
                return RiskLevel.valueOf(v);
            }
        } catch (Exception ignore) {
            String upper = raw.trim().toUpperCase();
            if (upper.contains("HIGH")) return RiskLevel.HIGH;
            if (upper.contains("MEDIUM")) return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public InterventionType classifyIntervention(String userMessage) {
        String raw = clovaHttpClient.callJson(
                clovaProperties.getEndpoint().getClassifyIntervention(),
                clovaProperties.getRequestId().getClassifyIntervention(),
                clovaHttpClient.buildBody(List.of(
                        Map.of("role", "system", "content", promptLoader.getClassifierIntervention()),
                        Map.of("role", "user", "content", userMessage)
                ), TEMPERATURE, MAX_TOKENS));
        if (raw == null) return InterventionType.SYMPATHY_SUPPORT;
        InterventionType type = InterventionType.fromCode(raw.trim());
        return type != null ? type : InterventionType.SYMPATHY_SUPPORT;
    }
}
