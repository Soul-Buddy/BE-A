-- ============================================================
-- Soul Buddy — 통합 DB 스키마 (최종 v2.1)
-- 기준: 02_db_schema.md + AI 기능명세 + soul_buddy_schema_revised.sql 반영
-- 작성: 2026-03-17
-- 변경(v2.1): MVP 범위 확정 반영
--   - provider: LOCAL/NAVER 제거 → GOOGLE/KAKAO만 지원
--   - password_hash 제거 (소셜 로그인 전용)
--   - preferred_tone: NULL → NOT NULL (온보딩 필수값)
--   - sender 컬럼명 확정 (role → sender 통일)
-- ============================================================

CREATE DATABASE IF NOT EXISTS soul_buddy
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE soul_buddy;

-- ────────────────────────────────────────────
-- 1. users
-- ────────────────────────────────────────────
CREATE TABLE users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255)    NOT NULL UNIQUE,
    nickname      VARCHAR(100)    NOT NULL,
    provider      ENUM('GOOGLE','KAKAO') NOT NULL,
    provider_id   VARCHAR(255)    NOT NULL,
    role          VARCHAR(20)     NOT NULL DEFAULT 'USER', -- USER | ADMIN
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- ────────────────────────────────────────────
-- 2. profiles
-- 온보딩 설문 결과 저장 (users 1:1)
-- hobbies, concerns, liked_things, disliked_things: JSON 배열 TEXT
-- personality: 자유 텍스트
-- preferred_tone: 프롬프트 블록② 말투 설정 필수값
-- ────────────────────────────────────────────
CREATE TABLE profiles (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL UNIQUE,
    nickname        VARCHAR(50)     NOT NULL,
    age             INT             NULL,
    hobbies         TEXT            NULL,                  -- JSON 배열 문자열
    personality     TEXT            NULL,
    concerns        TEXT            NULL,                  -- JSON 배열 문자열
    preferred_tone  VARCHAR(50)     NOT NULL,              -- CASUAL | FORMAL | EMPATHETIC (온보딩 필수)
    liked_things    TEXT            NULL,
    disliked_things TEXT            NULL,
    additional_info TEXT            NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- ────────────────────────────────────────────
-- 3. chat_sessions
-- id: UUID 문자열 (VARCHAR 36) — API contract 기준
-- persona_type: AI 페르소나 종류
-- status: 세션 종료 감지 → 요약 생성 트리거에 필수
-- ────────────────────────────────────────────
CREATE TABLE chat_sessions (
    id            VARCHAR(36)     NOT NULL,                -- UUID
    user_id       BIGINT          NOT NULL,
    persona_type  ENUM('FRIEND','COUNSELOR','EMPATHY') NOT NULL,
    status        ENUM('ACTIVE','ENDED')  NOT NULL DEFAULT 'ACTIVE',
    title         VARCHAR(255)    NULL,
    started_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at      TIMESTAMP       NULL,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    INDEX idx_chat_sessions_user_id (user_id),
    INDEX idx_chat_sessions_status (status)
);

-- ────────────────────────────────────────────
-- 4. chat_messages
-- sender: USER | ASSISTANT (전체 문서 통일 완료)
-- emotion_tag, risk_level: ASSISTANT 행에만 값 입력
-- ────────────────────────────────────────────
CREATE TABLE chat_messages (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    session_id  VARCHAR(36)     NOT NULL,
    sender      ENUM('USER','ASSISTANT') NOT NULL,
    content     TEXT            NOT NULL,
    emotion_tag ENUM('HAPPY','SAD','ANGRY','ANXIOUS','CALM','NEUTRAL') NULL,
    risk_level  ENUM('LOW','MEDIUM','HIGH') NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
        ON DELETE CASCADE,
    INDEX idx_chat_messages_session_id (session_id),
    INDEX idx_chat_messages_created_at (created_at)
);

-- ────────────────────────────────────────────
-- 5. summaries
-- memory_hint: 다음 세션 프롬프트 블록③ recentSummary 주입값 (핵심)
-- dominant_emotion: 해당 세션의 핵심 감정
-- emotion_change: 세션 내 감정 변화 흐름 (예: "불안 → 긴장 → 약간 안정")
-- user_id: dashboard API 조회 최적화용 역정규화
-- dominant_emotion, emotion_change, memory_hint: NULL 허용 (짧은 대화 등 예외 대응)
-- ────────────────────────────────────────────
CREATE TABLE summaries (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    session_id       VARCHAR(36) NOT NULL UNIQUE,
    user_id          BIGINT      NOT NULL,
    summary_text     TEXT        NOT NULL,
    dominant_emotion ENUM('HAPPY','SAD','ANGRY','ANXIOUS','CALM','NEUTRAL') NULL,
    emotion_change   VARCHAR(255) NULL,                      -- 세션 내 감정 변화 흐름 텍스트
    memory_hint      TEXT        NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_summaries_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_summaries_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    INDEX idx_summaries_user_id (user_id)
);

-- ────────────────────────────────────────────
-- 6. emotion_logs
-- MVP: 세션 단위 감정 집계. 문장별 전환 시 구조 변경 없이 insert 빈도만 증가
-- user_id: dashboard 감정 통계 집계 시 JOIN 없이 조회 가능
-- ────────────────────────────────────────────
CREATE TABLE emotion_logs (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    session_id  VARCHAR(36) NOT NULL,
    emotion_tag ENUM('HAPPY','SAD','ANGRY','ANXIOUS','CALM','NEUTRAL') NOT NULL,
    logged_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_emotion_logs_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_emotion_logs_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
        ON DELETE CASCADE,
    INDEX idx_emotion_logs_user_id (user_id),
    INDEX idx_emotion_logs_session_id (session_id),
    INDEX idx_emotion_logs_logged_at (logged_at)
);
