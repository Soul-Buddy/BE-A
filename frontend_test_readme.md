# Soul Buddy — Frontend ↔ Backend 통합 테스트 가이드

> 대상: 프런트엔드 담당자
> 목적: 이 레포(특히 `backend/`)를 받아서 로컬 BE를 띄우고, 메인 고민 상담 화면을 실제 API로 동작시켜보기 위한 단계별 가이드.
> 정본 계약: `swagger/SoulBuddy_API_v4.0.0.json` (Swagger UI / Postman / Stoplight 등 어디서든 import 가능)

⚠ 본 문서를 따라 했을 때 막히면 그 자리에서 끊고 알려주세요. 진행하면서 BE 코드를 바꿔야 할 일이 생기면 함께 조정합니다. **FE 코드는 임의로 수정하지 않습니다** (이건 BE 측의 합의 사항).

---

## 0. 한눈에 보기

| 항목 | 값 |
|------|----|
| BE Base URL (로컬) | `http://localhost:8080` |
| FE Origin (CORS 허용됨) | `http://localhost:3000` |
| 인증 방식 | `Authorization: Bearer <accessToken>` (JWT) |
| 응답 envelope | `{ success: boolean, data: any, error: { code, message } | null }` |
| Swagger 정본 | `swagger/SoulBuddy_API_v4.0.0.json` |
| 메인 채팅창 핵심 엔드포인트 | `POST /api/sessions` → `POST /api/chat` → `GET /api/chat/history/{id}` → `PATCH /api/sessions/{id}/end` |

---

## 1. 사전 준비

### 1-1. 런타임

| 도구 | 권장 버전 | 확인 |
|------|----------|------|
| JDK | 17 | `java -version` |
| MySQL | 8.0 | `mysql --version` 또는 Docker |
| Node | 20+ | `node -v` (FE 쪽) |

### 1-2. MySQL 띄우기 (둘 중 택1)

**옵션 A. Docker (가장 빠름)**
```bash
cd docs/envs
docker-compose up -d
# 확인
docker ps | grep soulbuddy-mysql
```

**옵션 B. 로컬 MySQL 사용**
- DB 이름: `soul_buddy` (언더스코어 포함)
- 사용자/비밀번호: `root` / `root` (또는 본인 환경에 맞춰 `.env` 수정)
- 명령:
  ```sql
  CREATE DATABASE soul_buddy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

> 스키마는 BE가 부팅 시 `application-local.yml`의 `ddl-auto: update`로 자동 생성합니다. 별도 SQL 실행 불필요.

### 1-3. backend/.env 작성

`backend/` 디렉터리에 `.env` 파일이 있어야 합니다. 템플릿은 `docs/envs/_env.example`. 최소 필수값:

```ini
# DB
DB_URL=jdbc:mysql://localhost:3306/soul_buddy?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
DB_USERNAME=root
DB_PASSWORD=root

# JWT (32자 이상)
JWT_SECRET=local-dev-secret-key-must-be-at-least-32-characters-long

# 서버
SERVER_PORT=8080
CORS_ORIGINS=http://localhost:3000

# CLOVA (메인 채팅창 실 동작에 필요. 키 없이도 기동은 됨)
CLOVA_API_KEY=
```

> CLOVA 키가 없으면 `POST /api/sessions` 첫 응답의 `openingMessage`와 `POST /api/chat`의 `assistantMessage`가 LLM 호출 실패(`AI_001`)로 떨어집니다. **메인 채팅창 모양을 검증할 때만 필요**하니, FE 레이아웃/네트워크 통합 단계에서는 빈 값으로 두고, 응답 스텁이 필요하면 BE 측에 요청해 주세요(임시 mock 모드 추가 가능).

### 1-4. (선택) OAuth 클라이언트

Google/Kakao OAuth는 **이번 통합 단계에서는 사용하지 않습니다.** 아래 §2-2의 `dev-login` 경로를 씁니다. 따라서 `GOOGLE_CLIENT_ID/SECRET`, `KAKAO_CLIENT_ID/SECRET`는 비워둬도 무방합니다(없으면 OAuth 리다이렉트만 막힐 뿐, 다른 API는 정상 동작).

---

## 2. 백엔드 띄우기

### 2-1. 부팅

```bash
cd backend
./gradlew bootRun
```

성공하면 콘솔에 `Started SoulBuddyApplication in X seconds` 가 찍히고 8080 포트가 LISTEN 상태가 됩니다. `application.yml`의 `spring.profiles.active: local`이 기본값이라 `application-local.yml`이 자동 적용됩니다.

### 2-2. 헬스체크

```bash
curl -s http://localhost:8080/api/health
# 기대: {"status":"UP"} 류의 응답
```

### 2-3. Swagger UI

부팅 후 브라우저에서:
```
http://localhost:8080/swagger-ui.html
```
springdoc 자동 생성 화면이 뜹니다. **단, 정본은 `swagger/SoulBuddy_API_v4.0.0.json`** 입니다(어노테이션이 빠진 일부 필드는 자동 생성에서 누락될 수 있음).

---

## 3. 개발용 토큰 발급 (`POST /api/auth/dev-login`)

OAuth 인가 코드 플로우를 우회해 **즉시 JWT를 받기 위한 임시 엔드포인트**입니다. 운영 배포 전에는 제거할 예정.

### 3-1. 호출

```bash
# body 생략 → email=dev@local.test / nickname="Dev User"로 자동 생성
curl -s -X POST http://localhost:8080/api/auth/dev-login \
     -H "Content-Type: application/json" \
     -d '{}'

# 또는 명시적으로
curl -s -X POST http://localhost:8080/api/auth/dev-login \
     -H "Content-Type: application/json" \
     -d '{"email":"frontend@local.test","nickname":"FE Tester"}'
```

### 3-2. 응답 예시

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1...",
    "refreshToken": "eyJhbGciOiJIUzI1...",
    "userId": 1,
    "isNewUser": true,
    "termsAgreed": false,
    "onboardingCompleted": false
  }
}
```

### 3-3. FE에서의 사용 패턴 (제안)

- 부팅 시 1회 호출 → 메모리/`localStorage`에 토큰 저장
- 이후 모든 요청에 `Authorization: Bearer ${accessToken}` 부착
- `httpOnly cookie`로 가도 좋고(NextAuth credentials provider), `localStorage`에 둬도 MVP에선 OK

> ⚠ 같은 email로 다시 호출하면 기존 user를 재사용하고 새 토큰을 발급합니다. `isNewUser=false`로 응답.

---

## 4. 메인 고민 상담 창 통합 시나리오

화면이 그려지는 정확한 호출 순서. **FE는 이 순서를 코드로 옮기면 됩니다.**

```
[부팅]
  POST /api/auth/dev-login            → accessToken 확보
  GET  /api/users/me                  → 닉네임/온보딩 여부 확인 (선택)

[채팅창 진입 — "고민 상담 시작" 클릭 시]
  POST /api/sessions                  → sessionId, openingMessage, recentSummary
        body: { "personaType": "FRIEND" } 또는 "COUNSELOR"
  (선택) PATCH /api/sessions/{sessionId}/pre-chat-emotion
        body: { "preChatEmotion": "ANXIOUS" }

[메시지 송수신 — 사용자가 입력하고 전송할 때마다]
  POST /api/chat
        body: { "sessionId", "personaType", "message", "recentSummary" }
        ※ recentSummary는 첫 메시지에만 동봉, 이후 null

[종료 — "끝내기" 탭]
  PATCH /api/sessions/{sessionId}/end → summaryText, dominantEmotion, memoryHint

[재진입 — 같은 세션의 기록 복원]
  GET /api/chat/history/{sessionId}?page=0&size=50
```

### 4-1. curl로 단계별 검증

토큰을 환경변수에 저장:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/dev-login \
         -H "Content-Type: application/json" -d '{}' \
         | python -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
echo $TOKEN
```

**(1) 세션 생성**
```bash
curl -s -X POST http://localhost:8080/api/sessions \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"personaType":"FRIEND"}'
```
응답 예:
```json
{
  "success": true,
  "data": {
    "sessionId": "8c9f1a3b-...",
    "personaType": "FRIEND",
    "openingMessage": "안녕! 오늘 하루 어땠어?",
    "recentSummary": null,
    "createdAt": "2026-05-08T10:00:00"
  }
}
```
`sessionId`를 변수로 저장:
```bash
SID="8c9f1a3b-..."   # 위 응답의 sessionId 복붙
```

**(2) (선택) 채팅 전 감정**
```bash
curl -s -X PATCH http://localhost:8080/api/sessions/$SID/pre-chat-emotion \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"preChatEmotion":"ANXIOUS"}'
```

**(3) 메시지 전송 (메인 동작)**
```bash
curl -s -X POST http://localhost:8080/api/chat \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d "{
           \"sessionId\":     \"$SID\",
           \"personaType\":   \"FRIEND\",
           \"message\":       \"요즘 시험 준비가 너무 힘들어서 잠도 잘 못 자.\",
           \"recentSummary\": null
         }"
```
응답에 다음이 포함되는지 확인:
- `data.assistantMessage` (string)
- `data.emotionTag` ∈ `HAPPY|SAD|ANGRY|ANXIOUS|HURT|EMBARRASSED`
- `data.riskLevel` ∈ `LOW|MEDIUM|HIGH`
- `data.interventionType` ∈ 11종 영문 코드 (또는 SYSTEM 안전 발화 시 `null`)
- `data.aiModel` ∈ `HCX-005-FRIEND|HCX-005-COUNSELOR|SYSTEM`
- `data.forcedSafety` (boolean)

**(4) 기록 조회**
```bash
curl -s "http://localhost:8080/api/chat/history/$SID?page=0&size=50" \
     -H "Authorization: Bearer $TOKEN"
```

**(5) 세션 종료 + 요약 생성**
```bash
curl -s -X PATCH http://localhost:8080/api/sessions/$SID/end \
     -H "Authorization: Bearer $TOKEN"
```

---

## 5. Safety 처리 (강제 안전 발화)

`POST /api/chat` 응답이 `forcedSafety=true`로 오면 FE는 **즉시 Safety Check 오버레이**를 띄우고 다음 이벤트를 기록해야 합니다.

```bash
# 오버레이가 뜬 직후 — BANNER_SHOWN 기록
curl -s -X POST http://localhost:8080/api/safety/events \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d "{
           \"sessionId\": \"$SID\",
           \"messageId\": null,
           \"eventType\": \"BANNER_SHOWN\",
           \"riskLevel\": \"HIGH\",
           \"resourceId\": null
         }"
```

### 5-1. 허용되는 eventType

`BANNER_SHOWN`, `ASSESSMENT_OPENED`, `ASSESSMENT_COMPLETED`, `CENTER_LIST_VIEWED`, `CENTER_CALL_TAPPED`

> `RISK_DETECTED` / `FORCED_SAFETY_REPLY`는 BE가 자체 기록하므로 FE에서 호출하면 `VALID_001` 거부됩니다.

### 5-2. 자가설문 제출
```bash
curl -s -X POST http://localhost:8080/api/safety/assessments \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d "{
           \"sessionId\": \"$SID\",
           \"assessmentType\": \"SUICIDE_RISK\",
           \"answers\": { \"q1\": \"yes\", \"q2\": \"no\" }
         }"
```

### 5-3. 상담센터 목록
```bash
curl -s "http://localhost:8080/api/counseling-centers?emergencyOnly=false"
# (인증 불필요)
```

### 5-4. forcedSafety 트리거 방법 (테스트용)

`application.yml`의 `soulbuddy.safety.forced-safety-threshold: 2` — 같은 세션에서 `riskLevel=HIGH`가 누적 N회 감지되면 강제 안전 발화로 전환됩니다. 자살/자해 관련 키워드를 2회 입력해 시나리오 확인 가능. 키워드/임계값은 BE 측에 요청하면 일시적으로 1로 낮춰드릴 수 있습니다.

---

## 6. 토큰 만료 → Refresh

```bash
curl -s -X POST http://localhost:8080/api/auth/refresh \
     -H "Content-Type: application/json" \
     -d "{\"refreshToken\":\"$REFRESH\"}"
# 응답: { success:true, data:{ accessToken: "..." } }
```

권장 FE 동작:
1. 모든 요청을 fetch wrapper로 감싸기.
2. 401 (`AUTH_001`) 수신 시 1회만 자동 refresh 호출 → 새 accessToken으로 원 요청 재시도.
3. refresh도 401이면 토큰 폐기 + 로그인 화면.

---

## 7. 에러 코드 매핑

| HTTP | code | 의미 / FE 처리 가이드 |
|------|------|---------------------|
| 400 | `VALID_001` | 요청 파라미터 유효성 오류 → 폼 토스트 |
| 401 | `AUTH_001` | 토큰 없음/만료 → refresh 1회 시도 |
| 401 | `AUTH_002`/`AUTH_003`/`AUTH_004` | 권한/토큰 무효 → 로그인 화면 |
| 404 | `SESSION_001` | 세션 없음 → 메인으로 |
| 409 | `SESSION_002` | 이미 종료된 세션 |
| 500 | `AI_001` | LLM 호출 실패 → "잠시 후 다시" 토스트, 재시도 버튼 |
| 500 | `AI_002` | 응답 파싱 실패 (BE가 fallback 반환) → 그대로 표시 |
| 500 | `RAG_001` | RAG 검색 실패 (BE가 RAG 건너뛰고 응답) → 그대로 표시 |
| 500 | `DB_001` | DB 저장 실패 |
| 500 | `COMMON_002` | 서버 내부 오류 |

전체 19종은 swagger의 `ErrorCode` enum을 참고.

---

## 8. FE 통합 체크리스트 (메인 채팅창 한정)

다음 8개를 새로 작성하면 `mockBuddyReply.ts` 의존이 사라지고 실제 BE와 통신합니다.

- [ ] `shared/api/httpClient.ts` — fetch wrapper. envelope 풀기, 401 시 refresh, `Authorization` 자동 부착
- [ ] `shared/api/tokenStore.ts` — access/refresh 토큰 저장/조회
- [ ] `shared/api/types.ts` — `ChatRequest`, `ChatResponse`, `SessionCreateResponse`, `EmotionTag`, `RiskLevel`, `InterventionType`, `Sender` 등 (수기 또는 `openapi-typescript` 자동 생성)
- [ ] `features/chat/select-buddy/api/createSession.ts` — `POST /api/sessions`
- [ ] `features/chat/send-message/api/postChat.ts` — `POST /api/chat`
- [ ] `features/chat/send-message/api/getHistory.ts` — `GET /api/chat/history/{id}`
- [ ] `features/chat/send-message/model/store.ts` 수정 — `scheduleMockReply` 자리에 `postChat()` 호출, `forcedSafety=true` 시 Safety 오버레이 + `BANNER_SHOWN` 기록
- [ ] `app/chat/[id]/page.tsx` 또는 `views/chat` 진입 처리 — 신규 sessionId면 `createSession`, 기존이면 `getHistory`로 시드

### 통합 테스트 시나리오

1. **정상 흐름** — "잠을 못 자" 입력 → 응답 표시 → emotionTag/riskLevel 확인
2. **강제 안전** — HIGH 키워드 2회 입력 → `forcedSafety=true` → 오버레이 노출 → `BANNER_SHOWN` POST 성공 (네트워크 탭)
3. **토큰 만료** — accessToken을 임의 변조 → 401 → refresh → 재호출 → 성공
4. **재진입** — 새로고침 후 같은 sessionId 진입 → 50건 history 로드
5. **세션 종료** — "끝내기" → summaryText 노출 → 같은 세션 다시 메시지 보내면 `SESSION_002` 거부

---

## 9. 디버깅

### 9-1. 흔한 실패와 해결

| 증상 | 원인 | 해결 |
|------|------|------|
| `Connection refused localhost:8080` | BE 미기동 | `./gradlew bootRun` 후 콘솔 확인 |
| CORS preflight 실패 | FE가 `http://localhost:3000`이 아님 | `CORS_ORIGINS` env 변경 또는 FE 포트 맞춤 |
| 401 `AUTH_001` 모든 요청 | 토큰 누락 | `Authorization: Bearer ...` 헤더 확인 |
| 500 `AI_001` `/api/sessions`/`/api/chat` | CLOVA 키/엔드포인트 미설정 | `.env`에 `CLOVA_API_KEY` 입력 또는 BE에 mock 모드 요청 |
| `Communications link failure` (DB) | MySQL 미기동 / DB명 오타 | `docker ps` 확인 / DB명 `soul_buddy` 확인 |
| 컴파일 에러 | JDK 17 미설치 | `java -version` 확인 |
| 포트 8080 점유 | 이전 java 프로세스 잔존 | `netstat -ano | findstr 8080` → 해당 PID `Stop-Process -Id <PID> -Force` |

### 9-2. BE 로그 위치

`backend/bootRun.log` (있으면) 또는 IDE/터미널 콘솔. `application-local.yml`에서 `com.soulbuddy: DEBUG`로 설정돼 있어 요청/응답이 자세히 찍힙니다.

### 9-3. DB 직접 확인

```bash
docker exec -it soulbuddy-mysql mysql -uroot -proot soul_buddy
mysql> SELECT id, email, nickname, created_at FROM users;
mysql> SELECT id, user_id, persona_type, status FROM chat_sessions ORDER BY id DESC LIMIT 5;
mysql> SELECT id, session_id, sender, LEFT(content, 30), emotion_tag, risk_level FROM chat_messages ORDER BY id DESC LIMIT 10;
```

---

## 10. 변경 시 알릴 것 (BE → FE)

다음이 바뀌면 swagger v4와 본 문서를 갱신하고 FE 측에 즉시 알립니다.

- `ChatResponse` 필드 추가/제거 또는 enum 변경 (특히 `emotionTag`, `interventionType`)
- `forcedSafety` 트리거 임계값 변경
- 엔드포인트 경로/HTTP method 변경
- 인증 방식 변경 (예: 쿠키 기반 전환)
- `recentSummary` 전달 정책 변경

---

## 11. 빠른 참조 — Postman/Insomnia 컬렉션을 만들 거라면

1. `swagger/SoulBuddy_API_v4.0.0.json` import.
2. Environment 변수 2개 추가:
   - `baseUrl = http://localhost:8080`
   - `accessToken = ` (dev-login 응답으로 채움)
3. Collection 인증을 `Bearer Token = {{accessToken}}`으로 일괄 설정.
4. 첫 번째 요청으로 `POST /api/auth/dev-login` 추가, Test 스크립트로 응답의 `data.accessToken`을 `accessToken` 변수에 저장:
   ```js
   const json = pm.response.json();
   pm.environment.set("accessToken", json.data.accessToken);
   pm.environment.set("refreshToken", json.data.refreshToken);
   ```
5. 이후 다른 요청은 자동으로 토큰이 부착됩니다.

---

*최종 수정: 2026-05-08 — `/api/auth/dev-login` 추가 + swagger v4 정본화 시점*
