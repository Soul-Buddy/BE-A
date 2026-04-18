package com.soulbuddy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LlmSingleCallTest {

    private static final String API_KEY = System.getenv("CLOVAX_API_KEY");
    private static final String URL = "https://clovastudio.stream.ntruss.com/v1/chat-completions/HCX-003";

    public static void main(String[] args) throws Exception {
        String body = """
                {
                  "messages": [{"role": "user", "content": "안녕하세요!"}],
                  "maxTokens": 200,
                  "temperature": 0.7,
                  "topK": 0,
                  "topP": 0.8,
                  "repeatPenalty": 5.0,
                  "stopBefore": [],
                  "includeAiFilters": true,
                  "seed": 0
                }
                """;

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=== HTTP 상태코드 ===");
        System.out.println(response.statusCode());
        System.out.println("=== HyperCLOVA X 응답 ===");
        System.out.println(response.body());
    }
}
