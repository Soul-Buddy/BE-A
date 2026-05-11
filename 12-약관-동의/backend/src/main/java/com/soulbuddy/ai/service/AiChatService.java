package com.soulbuddy.ai.service;

import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.domain.chat.entity.ChatMessage;

import java.util.List;

/**
 * AI 채팅 파이프라인 전담 서비스
 * PromptBuilder → LlmClient → AiResponseParser → SafetyFilter 순으로 처리
 * DB를 직접 호출하지 않습니다.
 */
public interface AiChatService {

    ChatResponse process(PromptContext context, List<ChatMessage> recentMessages);
}
