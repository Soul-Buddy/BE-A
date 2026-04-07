# Soul Buddy — AI 기능 명세서

*AI Functional Specification*

| **문서 버전** | **대상** | **원본 참조** |
|---|---|---|
| v2.2 (2026-03-31) | BE-A 담당 (장지훈), 바이브 코딩 AI 입력용 | 03_ai_spec.md, 04_persona_prompts.md |

---

## 1. AI 기능 범위

| **기능** | **설명** |
|---|---|
| AI 채팅 응답 생성 | 사용자 메시지 수신 → 컨텍스트 조립 → LLM 호출 → 구조화 응답 반환 |
| 감정 태깅 | 대화 내 감정 상태 자동 분류 (6종 Enum) — 참고용 추이 데이터 |
| 위험 감지 | 자해·자살 표현 1차(룰 기반) + 2차(LLM 판정) 필터링 |
| 대화 요약 | 세션 종료 시 핵심 내용·감정·기억 힌트 생성 |
| memoryHint 생성 | 다음 세션 시스템 프롬프트에 주입할 맥락 요약 ([사실]+[감정] 구조) |

⚠️ Soul Buddy는 의료 진단 서비스가 아닙니다. 프롬프트·응답 어디에도 진단·병명·처방 표현을 포함하지 않습니다.

---

## 2. Enum 클래스 정의

⚠️ 아래 Enum은 `ai/dto/` 또는 `global/` 패키지에 반드시 먼저 정의해야 합니다. ChatRequest, ChatResponse, SafetyFilter 모두 이 Enum을 공통으로 참조합니다.

```java
public enum PersonaType {
    FRIEND, COUNSELOR, EMPATHY
}

public enum EmotionTag {
    ANXIOUS, SAD, CALM, HAPPY, NEUTRAL, ANGRY
}

public enum RiskLevel {
    LOW, MEDIUM, HIGH
}
```

---

## 3. 입력 DTO — ChatRequest

```java
public class ChatRequest {
    private String sessionId;           // 현재 세션 UUID
    private PersonaType personaType;    // FRIEND / COUNSELOR / EMPATHY
    private String message;             // 사용자 입력 (최대 1000자)
    private String recentSummary;       // 이전 세션의 memoryHint 값 (없으면 null)
    // ⚠️ userId는 포함하지 않음 — JWT SecurityContext에서 추출
}
```

**recentSummary vs memoryHint 역할 구분**

- **recentSummary (입력)**: DB에 저장된 이전 세션의 memoryHint 값을 프론트에서 전달. PromptBuilder 블록 ③에 주입됨.
- **memoryHint (출력)**: 이번 세션 응답에서 LLM이 생성. 다음 요청 시 recentSummary로 재사용됨.
- 두 필드는 서로 다른 시점의 값이며, PromptBuilder에서 중복 주입하지 않습니다.

---

## 4. 출력 DTO — ChatResponse

```java
public class ChatResponse {
    private String assistantMessage;    // AI 대화 응답 메시지
    private EmotionTag emotionTag;      // ANXIOUS|SAD|CALM|HAPPY|NEUTRAL|ANGRY
    private RiskLevel riskLevel;        // LOW | MEDIUM | HIGH
    private String summary;             // 일반 채팅에서는 항상 null. AiSummaryService 전용.
    private String memoryHint;          // 다음 세션 컨텍스트 주입용 (200자 이하로 truncate)
    private String recommendedAction;   // HIGH 시 전문기관 안내 문구, 그 외 null
}
```

⚠️ `riskLevel = HIGH`이면 `recommendedAction`은 반드시 non-null입니다.

⚠️ `summary` 필드는 `POST /api/chat` 응답에서 항상 null입니다. 세션 종료(`PATCH /api/sessions/{sessionId}/end`) 후 AiSummaryService가 별도 생성합니다.

⚠️ `memoryHint`는 `[사실]...[감정]...` 형식이며, AiResponseParser에서 200자 초과 시 강제 truncate합니다.

⚠️ 필드명을 임의로 변경하지 마세요. 변경 시 `01_api_contract.md` 먼저 수정 후 팀 전체 합의 필요.

---

## 5. AI 처리 파이프라인

| **단계** | **클래스** | **역할** |
|---|---|---|
| 1 | ChatController | ChatRequest 수신 (userId는 JWT에서 추출) |
| 2 | SafetyFilter.preCheck() | 1차 룰 기반 HIGH 키워드 감지 → HIGH 시 즉시 SafetyResponse 반환 (6절 참조) |
| 3 | PromptBuilder.build(context) | 프로필 + 페르소나 + recentSummary 조립 (**PromptContext를 입력으로 받음**) |
| 4 | LlmClient.call() | OpenAI GPT-4o API 비동기 호출 (타임아웃 30초, 재시도 2회), 실패 시 fallback |
| 5 | AiResponseParser.parse() | JSON 파싱 + 스키마 검증 + memoryHint 200자 truncate, 파싱 실패 시 fallback 반환 |
| 6 | SafetyFilter.postProcess() | 2차 LLM 판정 riskLevel 검증 및 처리 |
| 7 | ChatResponse 반환 | 프론트엔드에 구조화된 응답 전달 |

---

## 6. AiChatService 핵심 분기 흐름 (Pseudo-code)

⚠️ 바이브 코딩 AI가 서비스 레이어 분기 로직을 임의로 구현하지 않도록 아래 흐름을 기준으로 작성합니다.

```java
// AiChatService.process()
public ChatResponse process(ChatRequest request, PromptContext context) {

    // 1차 안전 필터
    if (safetyFilter.preCheck(request.getMessage()) == RiskLevel.HIGH) {
        return buildSafetyResponse(); // LLM 호출 없이 즉시 반환
    }

    // 프롬프트 조립 — ⚠️ PromptContext를 입력으로 받음 (ChatRequest 아님)
    String systemPrompt = promptBuilder.build(context);

    // LLM 호출 (실패 시 fallback 반환)
    String rawJson = llmClient.call(systemPrompt, request.getMessage())
        .onErrorReturn(AiResponseParser.FALLBACK_JSON);

    // 파싱 (실패 시 fallback 반환, memoryHint 200자 truncate 포함)
    ChatResponse response = aiResponseParser.parse(rawJson);

    // 2차 안전 후처리
    return safetyFilter.postProcess(response);
}

// HIGH 즉시 반환용 응답 조립 — AiChatService 내부 private 메서드
private ChatResponse buildSafetyResponse() {
    ChatResponse res = new ChatResponse();
    res.setAssistantMessage(SafetyFilter.SAFETY_MESSAGE);
    res.setEmotionTag(EmotionTag.NEUTRAL);
    res.setRiskLevel(RiskLevel.HIGH);
    res.setMemoryHint(null);
    res.setRecommendedAction(SafetyFilter.SAFETY_ACTION);
    res.setSummary(null);
    return res;
}
```

---

## 7. LlmClient 메서드 시그니처

```java
public interface LlmClient {
    /**
     * @param systemPrompt PromptBuilder가 조립한 시스템 프롬프트 문자열
     * @param userMessage  사용자 입력 메시지
     * @return LLM이 반환한 raw JSON 문자열 (Mono)
     */
    Mono<String> call(String systemPrompt, String userMessage);
}
```

- 반환값은 파싱 전 raw JSON String입니다. 파싱은 AiResponseParser가 담당합니다.
- 타임아웃: 30초 / 재시도: 최대 2회
- 호출 실패(timeout, 4xx, 5xx) 시 `Mono.error` 반환 → AiChatService에서 `.onErrorReturn(FALLBACK_JSON)` 처리

---

## 8. AiResponseParser 메서드 시그니처

```java
public class AiResponseParser {

    // LLM 호출 실패 시 onErrorReturn에 사용할 fallback raw JSON 상수
    public static final String FALLBACK_JSON =
        "{\"assistantMessage\":\"죄송해요, 지금 잠시 연결이 불안정해요.\"," +
        "\"emotionTag\":\"NEUTRAL\",\"riskLevel\":\"LOW\",\"memoryHint\":null}";

    // LLM raw JSON → ChatResponse 변환. 파싱 실패 시 fallback() 반환 (예외 전파 없음)
    // memoryHint가 200자 초과 시 truncate 처리 포함
    public ChatResponse parse(String rawJson) { ... }

    // 파싱 불가 상황에서 안전한 기본 응답 반환
    public ChatResponse fallback() { ... }
}
```

- `parse()` 내부에서 try-catch 처리 — 파싱 실패 시 `fallback()` 호출, 예외 전파 없음
- `parse()` 내부에서 memoryHint 200자 초과 시 강제 truncate

| **에러 코드** | **상황** |
|---|---|
| AI_001 | LLM API 호출 실패 |
| AI_002 | 응답 파싱 실패 — fallback 반환됨 |

---

## 9. LLM API 요청 형식

> ⚠️ **OpenAI GPT-4o 단일 사용 확정.** 다른 LLM API 코드를 작성하지 마세요.

```json
{
  "model": "gpt-4o",
  "messages": [
    { "role": "system", "content": "<PromptBuilder 조립 결과>" },
    { "role": "user",   "content": "<사용자 메시지>" }
  ],
  "temperature": 0.7,
  "max_tokens": 800,
  "response_format": { "type": "json_object" }
}
```

---

## 10. AI 응답 JSON 스키마 (시스템 프롬프트 블록 ④)

```
[응답 형식 규칙]
반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트를 포함하지 마세요.
{
  "assistantMessage": "사용자에게 전달할 대화 메시지",
  "emotionTag": "ANXIOUS | SAD | CALM | HAPPY | NEUTRAL | ANGRY 중 하나",
  "riskLevel": "LOW | MEDIUM | HIGH 중 하나",
  "memoryHint": "[사실] 객관적 상황 정보 [감정] 감정 흐름 요약 (없으면 null, 최대 200자)"
}
```

---

## 11. 요약 생성 (세션 종료 시)

**호출 트리거**

`PATCH /api/sessions/{sessionId}/end` 엔드포인트 처리 시 ChatService(또는 SessionService)가 `AiSummaryService.summarize(sessionId)`를 호출합니다.

```java
// ChatService 또는 SessionService 내부
public SummaryResponse endSession(String sessionId) {
    List<ChatMessage> messages = chatMessageRepository.findBySessionId(sessionId);
    return aiSummaryService.summarize(sessionId, messages);
}
```

**반환 스키마**

```json
{
  "summaryText": "대화의 핵심 내용 2~3문장",
  "dominantEmotion": "ANXIOUS | SAD | CALM | HAPPY | NEUTRAL | ANGRY 중 하나",
  "memoryHint": "[사실] 객관적 상황 정보 [감정] 감정 흐름 요약 (없으면 null, 최대 200자)"
}
```

> *생성된 memoryHint는 DB의 해당 세션 Summary 엔티티에 저장되며, 다음 세션 시작 시 `POST /api/sessions` 응답의 `recentSummary`로 프론트에 전달됩니다.*

---

## 12. Fallback 처리

```java
fallback.setAssistantMessage("죄송해요, 지금 잠시 연결이 불안정해요. 잠깐 후에 다시 이야기해요.");
fallback.setEmotionTag(EmotionTag.NEUTRAL);
fallback.setRiskLevel(RiskLevel.LOW);
fallback.setMemoryHint(null);
fallback.setRecommendedAction(null);
fallback.setSummary(null);
```

---

## 13. 토큰 및 비용 관리

| **항목** | **기준** |
|---|---|
| 사용자 메시지 최대 | 1,000자 |
| 시스템 프롬프트 목표 | 500토큰 이하 |
| max_tokens | 800 |
| personality truncate | 100자 (PromptContext 조립 시) |
| recentSummary truncate | 200자 (PromptContext 조립 시) |
| memoryHint truncate | 200자 (AiResponseParser에서) |
| 응답 시간 로그 | Slf4j INFO 레벨 필수 |
| 토큰 사용량 로그 | usage.prompt_tokens, usage.completion_tokens 필수 |

⚠️ 전체 프로필 원문을 프롬프트에 그대로 넣지 마세요. PromptContext에서 요약·truncate 후 주입.

---

## 14. 패키지 구조 (ai/ 모듈)

| **클래스** | **역할** |
|---|---|
| ai/client/LlmClient.java | WebClient 기반 OpenAI GPT-4o API 비동기 호출 |
| ai/prompt/PromptBuilder.java | 프롬프트 4블록 조립기 (입력: PromptContext) |
| ai/parser/AiResponseParser.java | JSON 파싱 + fallback + memoryHint 200자 truncate |
| ai/filter/SafetyFilter.java | 위험 감지 1·2차 필터 |
| ai/service/AiChatService.java | 채팅 파이프라인 조율 (DB 직접 호출 안 함) |
| ai/service/AiSummaryService.java | 세션 종료 요약 생성 |
| ai/dto/ChatRequest.java | 채팅 요청 DTO (userId 미포함) |
| ai/dto/ChatResponse.java | 채팅 응답 DTO |
| ai/dto/PromptContext.java | PromptBuilder 전용 입력 DTO |

---

*문서 버전: v2.2 | 최종 수정: 2026-03-31*
