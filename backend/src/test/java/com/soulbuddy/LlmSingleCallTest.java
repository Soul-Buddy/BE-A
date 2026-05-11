package com.soulbuddy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * CLOVA Studio HCX-005 단일 호출 검증용 main.
 * 실행 전 환경변수 CLOVA_API_KEY 필요. 없으면 즉시 종료.
 * (코드에 키를 하드코딩하지 마세요. 키는 .env 파일에서 주입됩니다.)
 */
public class LlmSingleCallTest {

    private static final String API_KEY = System.getenv("CLOVA_API_KEY");
    private static final String URL = "https://clovastudio.stream.ntruss.com/v3/chat-completions/HCX-005";

    public static void main(String[] args) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("환경변수 CLOVA_API_KEY가 설정되지 않았습니다. .env 또는 시스템 환경변수에 설정 후 다시 실행하세요.");
            System.exit(1);
        }

        String body = """
                {
                  "messages": [
                    {"role": "system", "content": "당신은 따뜻한 정서 지원 AI 입니다."},
                    {"role": "user", "content": "안녕하세요. 오늘 좀 우울해요."}
                  ],
                  "topP": 0.8,
                  "topK": 0,
                  "maxTokens": 300,
                  "temperature": 0.5,
                  "repetitionPenalty": 1.1,
                  "stop": [],
                  "seed": 0,
                  "includeAiFilters": true
                }
                """;

        HttpClient client = HttpClient.newHttpClient();

        String auth = API_KEY.startsWith("Bearer ") ? API_KEY : "Bearer " + API_KEY;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Authorization", auth)
                .header("X-NCP-CLOVASTUDIO-REQUEST-ID", "soul-buddy-test")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=== HTTP 상태코드 ===");
        System.out.println(response.statusCode());
        System.out.println("=== HyperCLOVA X HCX-005 응답 ===");
        System.out.println(response.body());
    }
}
