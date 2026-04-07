# 02. DB 스키마 정의서

> **AI 코딩 지침**: 이 파일에 정의된 테이블 구조·컬럼명·타입을 기준으로 JPA Entity를 생성하세요.
> 임의로 컬럼을 추가·삭제하지 마세요. 변경 시 반드시 BE-C(헌영)와 협의 후 이 파일을 먼저 수정하세요.
>
> ⚠️ **정본(canonical source)**: `soul_buddy_schema_v2.sql`
> 이 문서와 SQL 파일이 충돌할 경우 SQL 파일을 우선합니다.

---

## 테이블 목록

| 테이블명 | 설명 | 담당 |
|----------|------|------|
| `users` | 회원 기본 정보 | BE-C |
| `profiles` | 온보딩 설문 결과 (1:1) | BE-C |
| `chat_sessions` | 채팅 세션 단위 | BE-C |
| `chat_messages` | 세션 내 개별 메시지 | BE-C |
| `summaries` | 세션 종료 시 요약 결과 | BE-C |
| `emotion_logs` | 세션별 감정 태그 이력 | BE-C |

---

## 테이블 상세

### `users`

```sql
CREATE TABLE users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255)    NOT NULL UNIQUE,
    nickname      VARCHAR(100)    NOT NULL,
    provider      ENUM('GOOGLE','KAKAO') NOT NULL,
    provider_id   VARCHAR(255)    NOT NULL,
    role          VARCHAR(20)     NOT NULL DEFAULT 'USER',   -- USER | ADMIN
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
```

> ⚠️ MVP에서는 소셜 로그인(GOOGLE, KAKAO)만 지원합니다. LOCAL 로그인, password_hash 컬럼은 없습니다.

---

### `profiles`

```sql
CREATE TABLE profiles (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL UNIQUE,     -- FK → users.id
    nickname        VARCHAR(50)     NOT NULL,
    age             INT             NULL,
    hobbies         TEXT            NULL,                 -- JSON 배열 문자열
    personality     TEXT            NULL,
    concerns        TEXT            NULL,                 -- JSON 배열 문자열
    preferred_tone  VARCHAR(50)     NOT NULL,             -- CASUAL | FORMAL | EMPATHETIC (온보딩 필수)
    liked_things    TEXT            NULL,                 -- JSON 배열 문자열
    disliked_things TEXT            NULL,                 -- JSON 배열 문자열
    additional_info TEXT            NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

> ⚠️ `hobbies`, `concerns`, `liked_things`, `disliked_things`는 JSON 배열을 TEXT로 저장합니다.
> 애플리케이션 레이어(JPA `@Convert`)에서 `List<String>` ↔ `String` 변환 처리하세요.
>
> ⚠️ `preferred_tone`은 **NOT NULL**입니다. 온보딩 시 반드시 선택해야 합니다.

---

### `chat_sessions`

```sql
CREATE TABLE chat_sessions (
    id            VARCHAR(36)     NOT NULL,               -- UUID
    user_id       BIGINT          NOT NULL,               -- FK → users.id
    persona_type  ENUM('FRIEND','COUNSELOR','EMPATHY') NOT NULL,
    status        ENUM('ACTIVE','ENDED') NOT NULL DEFAULT 'ACTIVE',
    title         VARCHAR(255)    NULL,
    started_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at      TIMESTAMP       NULL,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_chat_sessions_user_id (user_id),
    INDEX idx_chat_sessions_status (status)
);
```

---

### `chat_messages`

```sql
CREATE TABLE chat_messages (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    session_id  VARCHAR(36)     NOT NULL,               -- FK → chat_sessions.id
    sender      ENUM('USER','ASSISTANT') NOT NULL,      -- ⚠️ 컬럼명: sender (role 아님)
    content     TEXT            NOT NULL,
    emotion_tag ENUM('HAPPY','SAD','ANGRY','ANXIOUS','CALM','NEUTRAL') NULL,
    risk_level  ENUM('LOW','MEDIUM','HIGH') NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    INDEX idx_chat_messages_session_id (session_id),
    INDEX idx_chat_messages_created_at (created_at)
);
```

> ⚠️ **컬럼명은 `sender`입니다. `role`이 아닙니다.** JPA Entity에서도 `sender`로 통일하세요.
> `emotion_tag`, `risk_level`은 ASSISTANT 행에만 값을 입력합니다.

---

### `summaries`

```sql
CREATE TABLE summaries (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    session_id       VARCHAR(36)     NOT NULL UNIQUE,    -- FK → chat_sessions.id
    user_id          BIGINT          NOT NULL,           -- FK → users.id (빠른 조회용 역정규화)
    summary_text     TEXT            NOT NULL,
    dominant_emotion ENUM('HAPPY','SAD','ANGRY','ANXIOUS','CALM','NEUTRAL') NULL,
    emotion_change   VARCHAR(255)    NULL,               -- 세션 내 감정 변화 흐름 (예: "불안 → 긴장 → 약간 안정")
    memory_hint      TEXT            NULL,               -- 다음 세션 컨텍스트 주입용
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_summaries_user_id (user_id)
);
```

---

### `emotion_logs`

```sql
CREATE TABLE emotion_logs (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,               -- FK → users.id
    session_id  VARCHAR(36)     NOT NULL,               -- FK → chat_sessions.id
    emotion_tag ENUM('HAPPY','SAD','ANGRY','ANXIOUS','CALM','NEUTRAL') NOT NULL,
    logged_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    INDEX idx_emotion_logs_user_id (user_id),
    INDEX idx_emotion_logs_session_id (session_id),
    INDEX idx_emotion_logs_logged_at (logged_at)
);
```

---

## ERD 관계 요약

```
users (1) ──── (1) profiles
users (1) ──── (N) chat_sessions
users (1) ──── (N) summaries
users (1) ──── (N) emotion_logs

chat_sessions (1) ──── (N) chat_messages
chat_sessions (1) ──── (1) summaries
chat_sessions (1) ──── (N) emotion_logs
```

---

## JPA Entity 작성 시 주의사항

- 모든 Entity에 `@CreationTimestamp`, `@UpdateTimestamp` 적용
- `profiles`의 `List<String>` 필드는 `@Convert(converter = StringListConverter.class)` 사용
- `chat_sessions.id`는 UUID 문자열 — `@Id @Column(columnDefinition = "VARCHAR(36)")` 사용
- `chat_messages`의 컬럼명은 `sender` — `@Enumerated(EnumType.STRING) @Column(name = "sender")`
- 모든 FK에 `ON DELETE CASCADE` 적용
- Soft Delete 미사용 (캡스톤 범위), 데이터는 물리 삭제

---

*문서 버전: v1.1 | 최종 수정: 2026-03-31*
