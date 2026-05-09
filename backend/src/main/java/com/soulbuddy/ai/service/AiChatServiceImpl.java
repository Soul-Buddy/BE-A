package com.soulbuddy.ai.service;

import com.soulbuddy.ai.client.ClassifierClient;
import com.soulbuddy.ai.client.PersonaLlmClient;
import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.ClassificationResult;
import com.soulbuddy.ai.dto.OpeningContext;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.ai.filter.SafetyFilter;
import com.soulbuddy.ai.parser.AiResponseParser;
import com.soulbuddy.ai.prompt.PromptBuilder;
import com.soulbuddy.global.enums.RiskLevel;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ClassifierClient classifierClient;
    private final PersonaLlmClient personaLlmClient;
    private final PromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;
    private final SafetyFilter safetyFilter;

    @Value("${soulbuddy.safety.forced-safety-threshold:2}")
    private int forcedSafetyThreshold;

    @Override
    public ChatResponse process(ChatRequest request, PromptContext context, long recentHighCount) {
        long start = System.currentTimeMillis();

        boolean immediateHigh = safetyFilter.isImmediateHighRisk(request.getMessage());

        ClassificationResult classification = classifierClient.classifyParallel(request.getMessage());

        boolean forced = safetyFilter.decideForcedSafety(
                immediateHigh, classification.getRisk(), recentHighCount, forcedSafetyThreshold);

        if (forced) {
            ChatResponse safety = aiResponseParser.safetyResponse();
            safety.setEmotionTag(classification.getEmotion());
            log.info("Forced safety reply triggered. immediateHigh={} classifiedRisk={} ({}ms)",
                    immediateHigh, classification.getRisk(), System.currentTimeMillis() - start);
            return safety;
        }

        PromptContext finalContext = PromptContext.builder()
                .personalInstruction(context.getPersonalInstruction())
                .nickname(context.getNickname())
                .personaType(context.getPersonaType())
                .recentSummary(context.getRecentSummary())
                .recentTurns(context.getRecentTurns())
                .classifiedEmotion(classification.getEmotion())
                .classifiedRisk(classification.getRisk())
                .classifiedIntervention(classification.getIntervention())
                .ragTop3(context.getRagTop3())
                .firstTurn(context.isFirstTurn())
                .openingMeta(context.getOpeningMeta())
                .build();

        String systemPrompt = promptBuilder.build(finalContext);

        String raw = personaLlmClient.call(
                request.getPersonaType(),
                systemPrompt,
                request.getMessage(),
                promptBuilder.recentTurnsTruncated(finalContext.getRecentTurns()));

        if (raw == null) {
            log.warn("Persona LLM 호출 실패 → fallback 반환");
            ChatResponse fallback = aiResponseParser.fallback();
            fallback.setEmotionTag(classification.getEmotion());
            fallback.setRiskLevel(classification.getRisk());
            fallback.setInterventionType(classification.getIntervention());
            return fallback;
        }

        String assistantMessage = aiResponseParser.sanitizeAssistantMessage(raw);

        if (classification.getRisk() == RiskLevel.MEDIUM) {
            assistantMessage = assistantMessage + SafetyFilter.MEDIUM_RECOMMEND;
        }

        ChatResponse response = ChatResponse.builder()
                .assistantMessage(assistantMessage)
                .emotionTag(classification.getEmotion())
                .riskLevel(classification.getRisk())
                .interventionType(classification.getIntervention())
                .ragUsed(finalContext.getRagTop3() != null && !finalContext.getRagTop3().isEmpty())
                .aiModel(request.getPersonaType().aiModelTag())
                .forcedSafety(false)
                .summary(null)
                .memoryHint(null)
                .recommendedAction(classification.getRisk() == RiskLevel.HIGH
                        ? SafetyFilter.SAFETY_ACTION : null)
                .build();

        log.info("Persona LLM 응답 완료. persona={} risk={} ({}ms)",
                request.getPersonaType(), classification.getRisk(),
                System.currentTimeMillis() - start);
        return response;
    }

    @Override
    public String openingMessage(OpeningContext ctx) {
        long start = System.currentTimeMillis();

        PromptContext promptContext = PromptContext.builder()
                .nickname(ctx.getNickname())
                .personaType(ctx.getPersonaType())
                .recentSummary(ctx.getRecentSummary())
                .firstTurn(true)
                .openingMeta(PromptContext.OpeningMeta.builder()
                        .visitState(ctx.getVisitState().name())
                        .priorTopic(ctx.getPriorTopic())
                        .priorSeverity(ctx.getPriorSeverity() != null ? ctx.getPriorSeverity().name() : null)
                        .priorUnresolved(ctx.isPriorUnresolved())
                        .build())
                .build();

        String systemPrompt = promptBuilder.build(promptContext);

        String raw = personaLlmClient.call(
                ctx.getPersonaType(),
                systemPrompt,
                "(인사말을 자율 생성하세요)",
                Collections.emptyList());

        if (raw == null) {
            log.warn("Opening LLM 호출 실패 → 기본 인사말 fallback");
            return ctx.getPersonaType() == com.soulbuddy.global.enums.PersonaType.FRIEND
                    ? "안녕! 오늘 하루 어땠어? 편하게 얘기해줘."
                    : "안녕하세요. 오늘 어떤 이야기를 나누고 싶으신가요?";
        }

        String message = aiResponseParser.sanitizeAssistantMessage(raw);
        log.info("Opening 생성 완료. persona={} visitState={} ({}ms)",
                ctx.getPersonaType(), ctx.getVisitState(), System.currentTimeMillis() - start);
        return message;
    }
}
