package com.soulbuddy.ai.service;

import com.soulbuddy.ai.client.LlmClient;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.ai.filter.SafetyFilter;
import com.soulbuddy.ai.parser.AiResponseParser;
import com.soulbuddy.ai.prompt.PromptBuilder;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.global.enums.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final AiResponseParser aiResponseParser;
    private final SafetyFilter safetyFilter;

    @Override
    public ChatResponse process(PromptContext context, List<ChatMessage> recentMessages) {
        String systemPrompt = promptBuilder.build(context);

        List<Map<String, String>> messages = buildMessages(systemPrompt, recentMessages);

        long startTime = System.currentTimeMillis();
        String rawJson;
        try {
            rawJson = llmClient.call(messages);
        } catch (Exception e) {
            log.warn("LLM 호출 실패, fallback 반환: {}", e.getMessage());
            return aiResponseParser.fallback();
        }
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("LLM 응답 시간: {}ms", elapsed);

        ChatResponse response = aiResponseParser.parse(rawJson);
        return safetyFilter.postProcess(response);
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, List<ChatMessage> recentMessages) {
        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (ChatMessage msg : recentMessages) {
            String role = msg.getSender() == Sender.USER ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", msg.getContent()));
        }

        return messages;
    }
}
