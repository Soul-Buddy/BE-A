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

    private final WebClient openAiWebClient;

    @Value("${ai.openai.model:gpt-4o}")
    private String model;

    @Value("${ai.openai.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${ai.openai.max-retries:2}")
    private int maxRetries;

    @Override
    public String call(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.7,
                "max_tokens", 800,
                "response_format", Map.of("type", "json_object")
        );

        try {
            return openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1)))
                    .map(response -> {
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    })
                    .block();
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_001);
        }
    }
}
