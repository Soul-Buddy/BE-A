# backend/CLAUDE.md — 백엔드 AI 코딩 지침

> 이 파일은 백엔드 코드 작성 시 Claude가 읽어야 할 컨텍스트입니다.
> 루트 `CLAUDE.md`와 함께 읽으세요.

---

## 패키지 구조

```
src/main/java/com/soulbuddy/
├── SoulBuddyApplication.java
│
├── global/
│   ├── config/
│   │   ├── WebClientConfig.java       // LLM API WebClient 빈 설정
│   │   ├── SecurityConfig.java        // JWT + OAuth 보안 설정
│   │   └── SwaggerConfig.java         // OpenAPI 3.0 설정
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── BusinessException.java
│   ├── response/
│   │   ├── ApiResponse.java           // 공통 응답 래퍼 { success, data, error }
│   │   └── ErrorCode.java             // AUTH_001, AI_001 등 에러 코드 enum
│   └── util/
│       └── StringListConverter.java   // JPA List<String> ↔ TEXT 변환
│
├── domain/
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   └── dto/
│   │       ├── LoginRequest.java
│   │       └── TokenResponse.java
│   │
│   ├── user/
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── entity/User.java
│   │   ├── repository/UserRepository.java
│   │   └── dto/UserResponse.java
│   │
│   ├── profile/
│   │   ├── controller/OnboardingController.java
│   │   ├── service/ProfileService.java
│   │   ├── entity/Profile.java
│   │   ├── repository/ProfileRepository.java
│   │   └── dto/
│   │       ├── OnboardingRequest.java
│   │       └── ProfileResponse.java
│   │
│   ├── chat/
│   │   ├── controller/ChatController.java
│   │   ├── service/ChatService.java
│   │   ├── entity/
│   │   │   ├── ChatSession.java
│   │   │   └── ChatMessage.java
│   │   ├── repository/
│   │   │   ├── ChatSessionRepository.java
│   │   │   └── ChatMessageRepository.java
│   │   └── dto/
│   │       ├── SessionCreateRequest.java
│   │       └── SessionResponse.java
│   │
│   ├── summary/
│   │   ├── entity/Summary.java
│   │   ├── repository/SummaryRepository.java
│   │   └── dto/SummaryResponse.java
│   │
│   └── dashboard/
│       ├── controller/DashboardController.java
│       ├── service/DashboardService.java
│       ├── entity/EmotionLog.java
│       ├── repository/EmotionLogRepository.java
│       ├── service/EmotionLogService.java    // ← 감정 로그 저장 전담
│       └── dto/DashboardResponse.java
│
└── ai/
    ├── client/
    │   └── LlmClient.java             // WebClient 기반 LLM API 호출 (OpenAI GPT-4o 전용)
    ├── prompt/
    │   └── PromptBuilder.java         // 프롬프트 4블록 조립기
    ├── parser/
    │   └── AiResponseParser.java      // JSON 파싱 + fallback + memoryHint 200자 truncate
    ├── filter/
    │   └── SafetyFilter.java          // 위험 감지 1·2차 필터
    ├── service/
    │   ├── AiChatService.java         // 채팅 파이프라인 조율
    │   └── AiSummaryService.java      // 세션 종료 요약 생성
    └── dto/
        ├── ChatRequest.java
        ├── ChatResponse.java
        └── PromptContext.java         // PromptBuilder 전용 입력 DTO
```

---

## 파트별 담당 패키지

| 파트 | 담당자 | 패키지 |
|------|--------|--------|
| A | 장지훈 | `ai/`, `domain/chat/` |
| B | 준영 | `domain/auth/`, `domain/user/`, `domain/profile/`, `global/response/`, `global/config/SwaggerConfig` |
| C | 헌영 | 모든 Entity·Repository, `global/config/` (DB설정), Docker·배포 |

---

## 주요 구현 규칙

### 공통 응답 포맷
```java
// 모든 Controller는 ApiResponse<T>를 반환합니다
@RestController
public class ChatController {
    @PostMapping("/api/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(...) {
        ChatResponse result = aiChatService.process(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
```

### 사용자 식별 — JWT에서 추출 (필수)
```java
// userId는 Request Body가 아닌 JWT SecurityContext에서 추출합니다.
// Body에 userId가 있어도 무시하고 JWT 기준으로 처리합니다.
Long userId = SecurityContextHolder.getContext()
    .getAuthentication().getPrincipal().getUserId();
```

### 환경변수 참조
```yaml
# application.yml — 값은 없음. 환경변수에서 주입
ai:
  openai:
    api-key: ${OPENAI_API_KEY}
    base-url: https://api.openai.com/v1
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### LlmClient 타임아웃 설정
```java
// WebClient 타임아웃: 30초 / 재시도: 최대 2회
// 실패 시 AiResponseParser.fallback() 호출
// OpenAI GPT-4o 전용 — 다른 LLM API 구현 금지
```

### EmotionLogService 역할
```java
// EmotionLogService는 dashboard 패키지에 위치합니다.
// ChatService가 AI 응답 저장 시 EmotionLogService.save()를 호출합니다.
// AiChatService는 DB를 직접 호출하지 않습니다.
```

---

## 참조 문서

| 작업 | 읽어야 할 docs 파일 |
|------|---------------------|
| API 엔드포인트 구현 | `docs/01_api_contract.md` |
| Entity 작성 | `docs/02_db_schema.md` |
| AI 파이프라인 | `docs/03_ai_spec.md` |
| 프롬프트 빌더 | `docs/04_persona_prompts.md` |
| 안전 필터 | `docs/05_safety_policy.md` |

---

*문서 버전: v1.1 | 최종 수정: 2026-03-31*
