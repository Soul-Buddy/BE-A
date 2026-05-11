package com.soulbuddy.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulbuddy.global.config.ClovaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLOVA Studio v3 chat-completions 공통 호출 래퍼.
 * 비스트리밍(JSON) 모드만 사용. 스트리밍은 후속 작업.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaHttpClient {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final WebClient clovaWebClient;
    private final ClovaProperties clovaProperties;

    public String callJson(String endpoint, String requestId, Map<String, Object> body) {
        try {
            JsonNode response = clovaWebClient.post()
                    .uri(endpoint)
                    .header("X-NCP-CLOVASTUDIO-REQUEST-ID", requestId)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(clovaProperties.getTimeoutSeconds()))
                    .retryWhen(Retry.backoff(clovaProperties.getMaxRetries(), Duration.ofMillis(500))
                            .filter(this::isRetryable))
                    .onErrorResume(e -> {
                        log.error("CLOVA 호출 실패 endpoint={} err={}", endpoint, e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response == null) return null;
            JsonNode status = response.get("status");
            if (status != null && status.has("code")
                    && !"20000".equals(status.get("code").asText())) {
                log.warn("CLOVA 비정상 응답 status={} body={}", status.toString(), response);
                return null;
            }
            JsonNode result = response.get("result");
            if (result == null) return null;
            JsonNode message = result.get("message");
            if (message == null) return null;
            JsonNode content = message.get("content");
            if (content == null) return null;
            return content.asText();
        } catch (Exception e) {
            log.error("CLOVA 호출 예외 endpoint={} err={}", endpoint, e.getMessage(), e);
            return null;
        }
    }

    public Map<String, Object> buildBody(List<Map<String, String>> messages,
                                         double temperature, int maxTokens) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", messages);
        body.put("topP", 0.8);
        body.put("topK", 0);
        body.put("maxTokens", maxTokens);
        body.put("temperature", temperature);
        body.put("repetitionPenalty", 1.1);
        body.put("stop", List.of());
        body.put("seed", 0);
        body.put("includeAiFilters", true);
        return body;
    }

    private boolean isRetryable(Throwable t) {
        String msg = t.getMessage();
        return msg != null && (msg.contains("timeout") || msg.contains("502") || msg.contains("503"));
    }
}
