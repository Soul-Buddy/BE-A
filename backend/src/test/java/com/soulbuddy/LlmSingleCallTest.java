package com.soulbuddy;

import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

public class LlmSingleCallTest {

    private static final String API_KEY = System.getenv("OPENAI_API_KEY");

    public static void main(String[] args) {
        WebClient client = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + API_KEY)
                .defaultHeader("Content-Type", "application/json")
                .build();

        Map<String, Object> body = Map.of(
                "model", "gpt-4o",
                "messages", List.of(Map.of("role", "user", "content", "안녕하세요!")),
                "temperature", 0.7
        );

        String result = client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    List<Map<String, Object>> choices =
                            (List<Map<String, Object>>) response.get("choices");
                    Map<String, Object> message =
                            (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                })
                .block();

        System.out.println("=== LLM 응답 ===");
        System.out.println(result);
    }
}
