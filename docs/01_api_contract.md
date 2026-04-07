# 01. API 계약서 (Frontend ↔ Backend)

> **AI 코딩 지침**: 이 파일에 정의된 엔드포인트·필드명·타입은 프론트-백엔드 간 계약입니다.
> 임의로 필드명을 변경하거나 타입을 바꾸지 마세요. 변경 시 반드시 팀 전체 합의 후 이 파일을 먼저 수정하세요.

---

## 공통 사항

### 응답 포맷

모든 API는 아래 포맷으로 응답합니다.

```json
// 성공
{
  "success": true,
  "data": { ... },
  "error": null
}

// 실패
{
  "success": false,
  "data": null,
  "error": {
    "code": "에러코드",
    "message": "사람이 읽을 수 있는 메시지"
  }
}
```

### 사용자 식별 규칙

> ⚠️ **userId는 Request Body에서 받지 않습니다.**
> 모든 인증 필요 API에서 userId는 JWT 토큰의 `SecurityContext`에서 추출합니다.
> 아래 API 명세에 userId가 Request Body에 포함된 것처럼 보이는 경우,
> 이는 "어떤 사용자의 요청인지"를 명시하기 위한 문서 표기이며,
> 실제 구현에서는 JWT에서 추출한 값을 사용합니다.

### 에러 코드 목록

| 코드 | 설명 |
|------|------|
| `AUTH_001` | 인증 토큰 없음 또는 만료 |
| `AUTH_002` | 권한 없음 |
| `USER_001` | 사용자를 찾을 수 없음 |
| `PROFILE_001` | 프로필 데이터 누락 |
| `AI_001` | LLM API 호출 실패 |
| `AI_002` | 응답 파싱 실패 (fallback 반환됨) |
| `AI_003` | 위험 감지 — 안전 응답 반환 |
| `DB_001` | 데이터 저장 실패 |
| `VALID_001` | 요청 파라미터 유효성 오류 |

---

## API 목록

### 인증 (Auth) — 담당: BE-B

#### `POST /api/auth/login`
소셜 OAuth 로그인 처리

**Request Body**
```json
{
  "provider": "GOOGLE | KAKAO",
  "code": "OAuth 인가 코드"
}
```

**Response data**
```json
{
  "accessToken": "string (JWT)",
  "refreshToken": "string (JWT)",
  "userId": 1,
  "isNewUser": true
}
```

---

#### `POST /api/auth/refresh`
Access Token 갱신

**Request Body**
```json
{ "refreshToken": "string" }
```

**Response data**
```json
{ "accessToken": "string" }
```

---

### 사용자·프로필 (User / Profile) — 담당: BE-B

#### `GET /api/users/me`
현재 로그인된 사용자 기본 정보 조회 (JWT 기반)

**Response data**
```json
{
  "userId": 1,
  "nickname": "string",
  "email": "string",
  "profileCompleted": true
}
```

---

#### `POST /api/onboarding`
온보딩 설문 제출 (최초 1회)

> userId는 JWT에서 추출합니다.

**Request Body**
```json
{
  "nickname": "string",
  "hobbies": ["string"],
  "personality": "string (최대 200자)",
  "concerns": ["string"],
  "preferredTone": "CASUAL | FORMAL | EMPATHETIC",
  "likedThings": ["string"],
  "dislikedThings": ["string"],
  "additionalInfo": "string | null"
}
```

**Response data**
```json
{ "profileId": 1, "profileCompleted": true }
```

---

#### `GET /api/profiles/me`
현재 사용자 프로필 조회 (JWT 기반)

**Response data**
```json
{
  "profileId": 1,
  "userId": 1,
  "nickname": "string",
  "hobbies": ["string"],
  "personality": "string",
  "concerns": ["string"],
  "preferredTone": "CASUAL | FORMAL | EMPATHETIC",
  "likedThings": ["string"],
  "dislikedThings": ["string"],
  "additionalInfo": "string | null"
}
```

---

#### `PUT /api/profiles/me`
현재 사용자 프로필 수정 (JWT 기반)

**Request Body**: `POST /api/onboarding`과 동일

---

### 페르소나 (Persona) — 담당: BE-B

#### `GET /api/personas`
사용 가능한 페르소나 목록 조회

**Response data**
```json
[
  {
    "personaType": "FRIEND",
    "displayName": "친구형",
    "description": "편하고 친근하게 대화해요",
    "toneSample": "야 그거 진짜 힘들었겠다~"
  },
  {
    "personaType": "COUNSELOR",
    "displayName": "상담사형",
    "description": "전문적이고 체계적으로 대화해요",
    "toneSample": "그 상황에서 어떤 감정을 느끼셨나요?"
  },
  {
    "personaType": "EMPATHY",
    "displayName": "공감형",
    "description": "따뜻하게 감정을 함께해요",
    "toneSample": "정말 많이 힘드셨겠어요..."
  }
]
```

---

### 채팅 세션 (Session) — 담당: BE-B (생성) + BE-A (AI 처리)

#### `POST /api/sessions`
새 채팅 세션 시작

> userId는 JWT에서 추출합니다.

**Request Body**
```json
{
  "personaType": "FRIEND | COUNSELOR | EMPATHY"
}
```

**Response data**
```json
{
  "sessionId": "uuid-string",
  "personaType": "FRIEND",
  "recentSummary": "string | null",
  "createdAt": "2026-03-14T10:00:00Z"
}
```

> ℹ️ `recentSummary`는 해당 사용자의 가장 최근 종료된 세션의 `memoryHint` 값입니다.
> 프론트엔드는 이 값을 받아 `POST /api/chat` 요청 시 `recentSummary` 필드에 그대로 전달합니다.
> 이전 세션이 없으면 `null`입니다.

---

#### `GET /api/sessions`
현재 사용자의 세션 목록 조회 (JWT 기반)

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `page` | int | N | 페이지 번호 (기본값: 0) |
| `size` | int | N | 페이지 크기 (기본값: 20, 최대: 50) |

**Response data**
```json
{
  "sessions": [
    {
      "sessionId": "uuid-string",
      "personaType": "FRIEND",
      "status": "ACTIVE | ENDED",
      "title": "string | null",
      "startedAt": "2026-03-14T10:00:00Z",
      "endedAt": "2026-03-14T10:30:00Z | null"
    }
  ],
  "totalCount": 15,
  "page": 0,
  "size": 20
}
```

---

#### `PATCH /api/sessions/{sessionId}/end`
세션 종료 (요약 생성 트리거)

**Response data**
```json
{
  "sessionId": "uuid-string",
  "summary": "오늘 시험 스트레스에 대한 이야기를 나눴습니다...",
  "dominantEmotion": "ANXIOUS",
  "memoryHint": "[사실] 시험 기간 스트레스 [감정] 대화 후 다소 안정",
  "endedAt": "2026-03-14T10:30:00Z"
}
```

---

### 채팅 (Chat) — 담당: BE-A

#### `POST /api/chat`
AI에게 메시지 전송 및 응답 수신

> userId는 JWT에서 추출합니다.

**Request Body**
```json
{
  "sessionId": "uuid-string",
  "personaType": "FRIEND | COUNSELOR | EMPATHY",
  "message": "string (최대 1000자)",
  "recentSummary": "string | null"
}
```

**Response data**
```json
{
  "assistantMessage": "string",
  "emotionTag": "ANXIOUS | SAD | CALM | HAPPY | NEUTRAL | ANGRY",
  "riskLevel": "LOW | MEDIUM | HIGH",
  "summary": "string | null",
  "memoryHint": "string | null",
  "recommendedAction": "string | null"
}
```

> ⚠️ `riskLevel = HIGH`일 때 `recommendedAction`은 반드시 non-null입니다.
> 프론트엔드는 HIGH 수신 시 안전 모달을 즉시 표시해야 합니다.

---

#### `GET /api/chat/history/{sessionId}`
특정 세션의 대화 기록 조회

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `page` | int | N | 페이지 번호 (기본값: 0) |
| `size` | int | N | 페이지 크기 (기본값: 50, 최대: 100) |

**Response data**
```json
{
  "sessionId": "uuid-string",
  "messages": [
    {
      "messageId": 1,
      "sender": "USER | ASSISTANT",
      "content": "string",
      "emotionTag": "string | null",
      "createdAt": "2026-03-14T10:05:00Z"
    }
  ],
  "totalCount": 42,
  "page": 0,
  "size": 50
}
```

> ⚠️ `sender` 필드입니다. `role`이 아닙니다. DB 컬럼명과 통일되었습니다.

---

### 요약·감정 로그 (Dashboard) — 담당: BE-C (저장) + BE-A (생성)

#### `GET /api/dashboard/me`
현재 사용자의 감정 이력 + 요약 카드 목록 조회 (JWT 기반)

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `page` | int | N | recentSummaries 페이지 번호 (기본값: 0) |
| `size` | int | N | recentSummaries 페이지 크기 (기본값: 10, 최대: 30) |

**Response data**
```json
{
  "userId": 1,
  "emotionStats": {
    "ANXIOUS": 5,
    "SAD": 3,
    "CALM": 7,
    "HAPPY": 2,
    "NEUTRAL": 4,
    "ANGRY": 1
  },
  "recentSummaries": [
    {
      "sessionId": "uuid-string",
      "date": "2026-03-14",
      "summary": "string",
      "dominantEmotion": "ANXIOUS",
      "personaType": "FRIEND"
    }
  ],
  "totalSummaryCount": 15,
  "page": 0,
  "size": 10
}
```

> ⚠️ **감정 통계 면책**: `emotionStats`는 LLM 기반 감정 태깅의 집계이며, 정확한 감정 진단이 아닌 **참고용 감정 추이**입니다.
> 프론트엔드에서 이 데이터를 표시할 때 "감정 추이 (참고용)" 등 면책 문구를 함께 표시하세요.

---

## recentSummary 전달 흐름 요약

```
1. POST /api/sessions 응답에서 recentSummary 수신 (이전 세션의 memoryHint)
2. 프론트엔드가 이 값을 state에 보관
3. POST /api/chat 요청 시 recentSummary 필드에 그대로 전달
4. 첫 번째 채팅 메시지에만 전달하면 충분 (이후 메시지에서는 null 가능)
```

---

*문서 버전: v1.1 | 최종 수정: 2026-03-31*
