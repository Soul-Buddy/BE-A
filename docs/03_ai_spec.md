# 03. AI 연동 스펙

> **AI 코딩 지침**: 이 파일은 LLM API 연동 전체 명세입니다.
> AI 관련 코드(`ai/*` 패키지) 작성 시 반드시 이 파일과 `04_persona_prompts.md`, `05_safety_policy.md`를 함께 읽으세요.

---

## AI 파이프라인 전체 흐름

```
[ChatController]
      │  ChatRequest DTO 수신 (userId는 JWT에서 추출)
      ▼
[ChatService.processChat()]
      │
      ├─ 1. 유저 메시지 DB 저장 (ChatMessage, sender=USER)
      │
      ├─ 2. SafetyFilter.preCheck()    ← 1차: 룰 기반 HIGH 키워드 감지
      │       │
      │       ├── HIGH 감지
      │       │     └─ buildSafetyResponse() 생성
      │       │     └─ AI 메시지 DB 저장 (sender=ASSISTANT, riskLevel=HIGH)
      │       │     └─ EmotionLogService.save()
      │       │     └─ 즉시 반환
      │       │
      │       └── LOW → 계속 진행
      │
      ├─ 3. PromptContext 조립 (ProfileRepository 조회)
      │
      ├─ 4. AiChatService.process(request, context)   ← AI 파이프라인 전담
      │       │
      │       ├─ PromptBuilder.build(context)          ← ⚠️ PromptContext를 입력으로 받음
      │       │       └─ 시스템 프롬프트 조립
      │       │           ① 공통 안전 규칙 블록
      │       │           ② 사용자 프로필 요약 블록 (PromptContext에서 주입, truncate 적용)
      │       │           ③ 세션 정보 블록 (페르소나 + recentSummary)
      │       │           ④ JSON 응답 형식 규칙 블록
      │       │
      │       ├─ LlmClient.call(systemPrompt, userMessage)
      │       │       └─ OpenAI GPT-4o API 비동기 호출
      │       │           타임아웃: 30초 / 재시도: 최대 2회
      │       │           실패 시 → AiResponseParser.fallback() 반환
      │       │
      │       ├─ AiResponseParser.parse(rawJson)
      │       │       └─ JSON 파싱 → ChatResponse DTO
      │       │           memoryHint 200자 초과 시 truncate
      │       │           파싱 실패 시 → fallback 응답 반환
      │       │
      │       └─ SafetyFilter.postProcess(response)
      │               └─ riskLevel 검증, HIGH 시 recommendedAction 주입
      │
      ├─ 5. AI 메시지 DB 저장 (ChatMessage, sender=ASSISTANT)
      │
      ├─ 6. EmotionLogService.save()   ← AI 응답의 emotionTag 기반 저장
      │
      └─ 7. ChatResponse 반환
```

> ⚠️ 메시지 저장과 EmotionLog 저장은 `ChatService` 책임입니다.
> `AiChatService`는 AI 파이프라인만 담당하며 DB를 직접 호출하지 않습니다.
>
> ⚠️ `EmotionLogService`는 `domain/dashboard/` 패키지에 위치합니다.

---

## ChatRequest DTO

```java
public class ChatRequest {
    private String sessionId;           // 현재 세션 UUID
    private PersonaType personaType;    // FRIEND / COUNSELOR / EMPATHY
    private String message;             // 사용자 입력 (최대 1000자)
    private String recentSummary;       // 이전 세션의 memoryHint 값 (없으면 null)
    // ⚠️ userId는 포함하지 않음 — JWT SecurityContext에서 추출
}
```

---

## ChatResponse DTO

```java
public class ChatResponse {
    private String assistantMessage;    // AI 대화 응답
    private String emotionTag;          // ANXIOUS | SAD | CALM | HAPPY | NEUTRAL | ANGRY
    private String riskLevel;           // LOW | MEDIUM | HIGH
    private String summary;             // 세션 종료 시에만 non-null, 일반 채팅에서는 항상 null
    private String memoryHint;          // 다음 세션에 주입할 1~2문장 (200자 이하로 truncate)
    private String recommendedAction;   // HIGH 시 전문기관 안내 문구 (고정값)
}
```

---

## PromptContext DTO — PromptBuilder 입력 구조

> ⚠️ `PromptBuilder.build()`는 반드시 아래 DTO를 입력으로 받습니다.
> `ChatRequest`나 `Profile`을 직접 받지 않습니다.
> `ChatService`에서 프로필을 조회하여 이 DTO를 조립한 후 `AiChatService`에 전달합니다.

```java
public class PromptContext {
    private String nickname;          // Profile.nickname
    private String preferredTone;     // Profile.preferredTone
    private String personality;       // Profile.personality (100자로 truncate)
    private List<String> hobbies;     // Profile.hobbies (최대 3개)
    private List<String> concerns;    // Profile.concerns (최대 3개)
    private String recentSummary;     // ChatRequest.recentSummary (200자로 truncate, 없으면 null)
    private PersonaType personaType;  // ChatRequest.personaType
}
```

### 세션 내 대화 히스토리 주입 규칙

- 현재 세션의 직전 메시지를 **최대 10개**까지 LLM messages 배열에 포함합니다.
- 10개 초과 시 가장 오래된 메시지부터 제외합니다.
- 포함 순서: 오래된 것 → 최신 순 (시간 오름차순).
- 오프닝 메시지(고정 문자열)도 히스토리에 포함합니다.

### 이전 세션 요약 압축 주입 규칙 (recentSummary)

- 직전 **최대 7개** 종료 세션의 요약을 백엔드에서 압축하여 `recentSummary` 단일 문자열로 주입합니다.
- 7개 미만이면 존재하는 세션 수만큼만 사용합니다.
- 각 세션에서 추출하는 3가지 요소:
  1. `summary_text` — 핵심 상황 요약
  2. `dominant_emotion` — 핵심 감정
  3. `emotion_change` — 세션 내 감정 변화 흐름
- 압축 형식 (세션 오래된 순 → 최신 순):

```
[과거 대화 요약]
(3회 전) 상황: {summary_text} / 핵심감정: {dominant_emotion} / 감정변화: {emotion_change}
(2회 전) 상황: {summary_text} / 핵심감정: {dominant_emotion} / 감정변화: {emotion_change}
(지난 대화) 상황: {summary_text} / 핵심감정: {dominant_emotion} / 감정변화: {emotion_change}
```

- 전체 압축 문자열은 **500자**를 초과하면 오래된 세션부터 제거합니다.
- 세션이 하나도 없으면 `recentSummary = null`.

---

### PromptContext 조립 시 Truncate 규칙

| 필드 | 최대 길이 | 처리 방법 |
|------|----------|----------|
| `personality` | 100자 | 100자 초과 시 잘라내고 "..." 추가 |
| `hobbies` | 3개 | 리스트에서 앞 3개만 사용 |
| `concerns` | 3개 | 리스트에서 앞 3개만 사용 |
| `recentSummary` | 200자 | 200자 초과 시 잘라내고 "..." 추가 |

> ⚠️ 이 truncate는 `ChatService`에서 PromptContext를 조립할 때 적용합니다.
> PromptBuilder는 이미 정제된 PromptContext를 받으므로 추가 처리 불필요합니다.

### PromptBuilder 시그니처

```java
public String build(PromptContext context)
```

---

## LLM API 요청 형식 — OpenAI GPT-4o

> ⚠️ 본 프로젝트는 **OpenAI GPT-4o** 단일 사용으로 확정되었습니다.
> 다른 LLM API 코드를 작성하지 마세요.
> 환경변수: `OPENAI_API_KEY` (코드 하드코딩 금지)

```json
{
  "model": "gpt-4o",
  "messages": [
    { "role": "system", "content": "<PromptBuilder가 조립한 시스템 프롬프트>" },
    { "role": "user",   "content": "<사용자 메시지>" }
  ],
  "temperature": 0.7,
  "max_tokens": 800,
  "response_format": { "type": "json_object" }
}
```

---

## AI에게 요청하는 JSON 응답 스키마

시스템 프롬프트 마지막 블록에 반드시 포함:

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

## 프롬프트 블록 조립 규칙 (PromptBuilder)

### 블록 ① — 공통 안전 규칙 (항상 포함, 첫 번째)
→ `docs/05_safety_policy.md` 참조

### 블록 ② — 사용자 프로필 요약 (항상 포함)

`PromptContext`의 값을 아래 형식으로 주입합니다.

```
[사용자 정보]
- 닉네임: {context.nickname}
- 선호 말투: {context.preferredTone}
- 성격: {context.personality} (최대 100자)
- 주요 취미: {context.hobbies (최대 3개)}
- 자주 언급하는 고민: {context.concerns (최대 3개)}
```

> ⚠️ 전체 프로필 원문을 그대로 넣지 마세요. 위 요약 형식으로 압축하세요.
> personality, hobbies, concerns는 PromptContext 조립 시 이미 truncate된 상태입니다.

### 블록 ③ — 세션 정보 (조건부 포함)

```
[현재 세션]
- 대화 모드: {context.personaType 설명}
- 이전 대화 요약: {context.recentSummary} (null이면 이 줄 생략, 최대 200자)
```

### 블록 ④ — 응답 형식 규칙 (항상 포함, 마지막)
→ 위 "AI에게 요청하는 JSON 응답 스키마" 참조

---

## Fallback 처리 규칙

AI 응답 파싱 실패 또는 API 호출 실패 시:

```java
// AiResponseParser.fallback()
ChatResponse fallback = new ChatResponse();
fallback.setAssistantMessage("죄송해요, 지금 잠시 연결이 불안정해요. 잠깐 후에 다시 이야기해요.");
fallback.setEmotionTag("NEUTRAL");
fallback.setRiskLevel("LOW");
fallback.setMemoryHint(null);
fallback.setRecommendedAction(null);
```

---

## AiResponseParser — memoryHint Truncate 규칙

```java
// parse() 내부에서 memoryHint 길이 제한 적용
if (response.getMemoryHint() != null && response.getMemoryHint().length() > 200) {
    response.setMemoryHint(response.getMemoryHint().substring(0, 200) + "...");
}
```

> ⚠️ LLM이 memoryHint를 장문으로 생성하는 경우를 방지합니다.
> 200자 초과 시 강제 truncate합니다.

---

## 감정 태깅 면책 사항

> `emotionTag`는 LLM이 대화 맥락에서 추론한 값이며, 동일 입력에 대해 결과가 달라질 수 있습니다.
> dashboard의 감정 통계(`emotionStats`)는 **참고용 감정 추이**로만 활용하며,
> 정확한 감정 진단 데이터가 아닙니다.
> 프론트엔드에서 감정 통계를 표시할 때 면책 문구를 함께 표시하세요.

---

## 토큰 · 비용 관리

- 사용자 메시지 최대: 1,000자
- 시스템 프롬프트 목표: 500토큰 이하
- `max_tokens`: 800
- 응답 시간 로그 필수 (Slf4j, INFO 레벨)
- 토큰 사용량 로그 필수 (`usage.prompt_tokens`, `usage.completion_tokens`)

---

*문서 버전: v1.2 | 최종 수정: 2026-03-31*
