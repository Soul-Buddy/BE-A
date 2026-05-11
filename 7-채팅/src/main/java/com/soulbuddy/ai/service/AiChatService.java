package com.soulbuddy.ai.service;

import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.OpeningContext;
import com.soulbuddy.ai.dto.PromptContext;

/**
 * AI 채팅 파이프라인 전담.
 * 흐름: 분류 3종 병렬 → SafetyGate → RAG 트리거 → PromptBuilder → 페르소나 호출
 * DB는 직접 호출하지 않습니다 (ChatService 책임).
 */
public interface AiChatService {

    ChatResponse process(ChatRequest request, PromptContext context, long recentHighCount);

    /**
     * 세션 시작 시 페르소나 LLM(HCX-005 ft)이 자율 생성하는 인사말.
     * production: 헌영 SessionService가 호출.
     */
    String openingMessage(OpeningContext context);
}
