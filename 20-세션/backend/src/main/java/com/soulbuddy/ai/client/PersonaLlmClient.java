package com.soulbuddy.ai.client;

import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.global.config.ClovaProperties;
import com.soulbuddy.global.enums.PersonaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HCX-005 ft (a/b) 페르소나 응답 호출.
 * - personaType=FRIEND  → 포코 (HCX-005 ft a)
 * - personaType=COUNSELOR → 루미 (HCX-005 ft b)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonaLlmClient {

    private static final int MAX_TOKENS = 505;
    private static final double TEMPERATURE = 0.5;

    private final ClovaHttpClient clovaHttpClient;
    private final ClovaProperties clovaProperties;

    public String call(PersonaType persona, String systemPrompt, String userMessage,
                       List<PromptContext.TurnMessage> recentTurns) {
        String endpoint = persona == PersonaType.FRIEND
                ? clovaProperties.getEndpoint().getPersonaFriend()
                : clovaProperties.getEndpoint().getPersonaCounselor();
        String requestId = persona == PersonaType.FRIEND
                ? clovaProperties.getRequestId().getPersonaFriend()
                : clovaProperties.getRequestId().getPersonaCounselor();

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (recentTurns != null) {
            for (PromptContext.TurnMessage t : recentTurns) {
                messages.add(Map.of("role", t.getRole(), "content", t.getContent()));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        return clovaHttpClient.callJson(endpoint, requestId,
                clovaHttpClient.buildBody(messages, TEMPERATURE, MAX_TOKENS));
    }
}
