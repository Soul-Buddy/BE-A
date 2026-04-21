package com.soulbuddy.ai.client;

import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {

    private final WebClient clovaxWebClient;

    @Value("${ai.clovax.model:HCX-003}")
    private String model;

    @Value("${ai.clovax.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${ai.clovax.max-retries:2}")
    private int maxRetries;

    @Override
    public String call(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = Map.of(
                "messages", messages,
                "maxTokens", 800,
                "temperature", 0.7,
                "topK", 0,
                "topP", 0.8,
                "repeatPenalty", 5.0,
                "stopBefore", List.of(),
                "includeAiFilters", true,
                "seed", 0
        );

        try {
            return clovaxWebClient.post()
                    .uri("/chat-completions/" + model)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1)))
                    .map(response -> {
                        Map<String, Object> result = (Map<String, Object>) response.get("result");
                        Map<String, Object> message = (Map<String, Object>) result.get("message");
                        return (String) message.get("content");
                    })
                    .block();
        } catch (Exception e) {
            log.error("HyperCLOVA X API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_001);
        }
    }
}
