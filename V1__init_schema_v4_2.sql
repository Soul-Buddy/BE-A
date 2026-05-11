-- ============================================================
-- Soul Buddy — DB 스키마 v4.2 (최소 필수 구성)
-- 기준: AI 로직 v2.1 (HyperCLOVA X) + 어플 흐름도 + 화면 데이터 명세 v1.0
-- 작성: 2026-05-02
--
-- [Flyway 마이그레이션]
--   V1 : 초기 스키마 전체 생성 (v4.2 기준)
--   이후 변경사항은 V2__설명.sql, V3__설명.sql ... 으로 추가할 것
--
-- v4.1 → v4.2 변경 요약
--   ① profiles          : gender ENUM → MALE/FEMALE 2종,
--                         personality·concerns 제거,
--                         preferred_tone VARCHAR → ENUM('FRIEND','COUNSELOR')
--   ② user_settings     : chat_time_limit_min 제거
--   ③ chat_sessions     : end_reason 제거,
--                         persona_type/status 주석 상세화 (포코/루미)
--   ④ chat_messages     : sender 각 값 상세 설명 추가
--   ⑤ emotion_logs      : intensity 제거
--   ⑥ safety_events     : matched_keyword 제거
--   ⑦ self_assessments  : score·risk_grade 제거,
--                         HIGH 시 강제 조치 정책 주석 추가
--   ⑧ rag_chunks        : chunk_type·emotion_meta·risk_meta 제거,
--                         pulling_text 추가
-- ============================================================

-- ────────────────────────────────────────────
-- 1. users
--   - 소셜 로그인 전용 (Google / Kakao)
--   - [01. 로그인] 신규/기존 분기
--   - terms_agreed_at    : [02. 약관동의] 통과 시점
--   - privacy_agreed_at  : 개인정보 수집·이용 동의 시점 (약관과 분리)
-- ────────────────────────────────────────────
CREATE TABLE users (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    nickname            VARCHAR(100)    NOT NULL,
    provider            ENUM('GOOGLE','KAKAO') NOT NULL,
    provider_id         VARCHAR(255)    NOT NULL,
    role                VARCHAR(20)     NOT NULL DEFAULT 'USER',
    terms_agreed_at     TIMESTAMP       NULL,
    privacy_agreed_at   TIMESTAMP       NULL,
    last_login_at       TIMESTAMP       NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_provider (provider, provider_id)
);

-- ────────────────────────────────────────────
-- 2. profiles
--   - [03. 온보딩 설문] 입력값 저장
--   - users 1:1
--   - hobbies / liked_things / disliked_things : JSON 배열 문자열
--   - preferred_tone         : AI 페르소나 선택 기준 (FRIEND=포코 | COUNSELOR=루미)
--   - personal_instruction   : 온보딩 데이터를 사전 가공한 프롬프트 캐싱 필드
--                              (매 턴 PromptBuilder 조립 비용 절감)
--   - onboarding_completed_at: 온보딩 완료 시점 (04번 라우팅 분기 기준)
-- ────────────────────────────────────────────
CREATE TABLE profiles (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT          NOT NULL UNIQUE,
    nickname                VARCHAR(50)     NOT NULL,
    age                     INT             NULL,
    gender                  ENUM('MALE','FEMALE') NULL,
    occupation              VARCHAR(100)    NULL,
    usage_intent            TEXT            NULL,
    hobbies                 TEXT            NULL,
    preferred_tone          ENUM('FRIEND','COUNSELOR') NOT NULL,
    liked_things            TEXT            NULL,
    disliked_things         TEXT            NULL,
    personal_instruction    TEXT            NULL,
    onboarding_completed_at TIMESTAMP       NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- ────────────────────────────────────────────
-- 3. user_settings
--   - [0X. 프로필/설정] 토글값 분리
--   - profiles와 분리해 잦은 update 비용 절감
--   - safety_resource_consent: 위기 시 상담기관 안내 동의
-- ────────────────────────────────────────────
CREATE TABLE user_settings (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT      NOT NULL UNIQUE,
    safety_resource_consent BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_settings_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- ────────────────────────────────────────────
-- 4. chat_sessions
--   - [05. 대화 선택] 페르소나 선택 시 생성
--   - persona_type  : FRIEND = 포코(친구형, 캐주얼·공감 중심)
--                     COUNSELOR = 루미(상담사형, 전문적·구조화 중심)
--   - status        : ACTIVE   = 현재 진행 중인 대화 세션
--                                (사용자가 채팅 화면에 있거나 앱을 일시 이탈한 상태 포함)
--                     ENDED    = 사용자가 '대화 종료'를 직접 선택하거나
--                                Safety HIGH 감지 후 강제 종료된 정상 완료 세션
--                                (요약 생성 대상)
--                     ABANDONED= 요약 생성 없이 중도 이탈된 세션
--                                (앱 강제 종료, 장시간 미응답 등으로 자동 처리)
--   - pre_chat_emotion: [06. 채팅 전 감정 파악] 선택값
--   - summary_status: NOT_CREATED(기본) → CREATED(요약 성공) / SKIPPED(중도퇴장)
--   - deleted_at    : 소프트 딜리트 — [08 기록보기] 삭제 스와이프 시 물리 삭제 대신 사용
-- ────────────────────────────────────────────
CREATE TABLE chat_sessions (
    id                  VARCHAR(36)     NOT NULL,
    user_id             BIGINT          NOT NULL,
    persona_type        ENUM('FRIEND','COUNSELOR') NOT NULL,
    status              ENUM('ACTIVE','ENDED','ABANDONED') NOT NULL DEFAULT 'ACTIVE',
    pre_chat_emotion    ENUM('HAPPY','SAD','ANGRY','ANXIOUS','HURT','EMBARRASSED') NULL,
    started_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at            TIMESTAMP       NULL,
    summary_status      ENUM('NOT_CREATED','CREATED','SKIPPED') NOT NULL DEFAULT 'NOT_CREATED',
    deleted_at          TIMESTAMP       NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    INDEX idx_chat_sessions_user_id (user_id),
    INDEX idx_chat_sessions_status (status),
    INDEX idx_chat_sessions_started_at (started_at),
    INDEX idx_chat_sessions_deleted_at (deleted_at)
);

-- ────────────────────────────────────────────
-- 5. chat_messages
--   - [07. 채팅] 매 턴 저장
--   - sender          : USER      = 실제 사용자가 입력한 메시지
--                                   (emotion_tag, risk_level, intervention_type 분류 대상)
--                       ASSISTANT = 포코(FRIEND) 또는 루미(COUNSELOR) 페르소나가 생성한 응답
--                                   (rag_used, ai_model 기록 대상)
--                       SYSTEM    = Safety Gate HIGH 감지 시 페르소나 응답 대신
--                                   강제 삽입되는 안전 발화 메시지
--                                   (일반 대화 흐름이 아닌 위기 개입 목적)
--   - DASH-002 분류기 3종 병렬 결과:
--       A. emotion_tag       : 감정 분류
--       B. risk_level        : 위험도 분류 → Safety Gate 입력
--       C. intervention_type : 개입유형 분류
--   - rag_used         : ASSISTANT 응답에 RAG 결과 주입 여부
--   - ai_model         : 응답 생성에 사용한 모델명 (HCX-005-FRIEND / HCX-005-COUNSELOR / HCX-007 등)
-- ────────────────────────────────────────────
CREATE TABLE chat_messages (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    session_id          VARCHAR(36) NOT NULL,
    sender              ENUM('USER','ASSISTANT','SYSTEM') NOT NULL,
    content             TEXT        NOT NULL,
    emotion_tag         ENUM('HAPPY','SAD','ANGRY','ANXIOUS','HURT','EMBARRASSED') NULL,
    risk_level          ENUM('LOW','MEDIUM','HIGH') NULL,
    intervention_type   ENUM('sympathy_support','clarification_reflection',
                             'structuring','goal_setting',
                             'cognitive_restructuring','behavioral_intervention',
                             'emotional_regulation_education_training',
                             'information_provision','process_feedback',
                             'task_assignment','training_of_coping_skills') NULL,
    rag_used            BOOLEAN     NOT NULL DEFAULT FALSE,
    ai_model            VARCHAR(50) NULL,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
        ON DELETE CASCADE,
    INDEX idx_chat_messages_session_id (session_id),
    INDEX idx_chat_messages_created_at (created_at),
    INDEX idx_chat_messages_risk_level (risk_level)
);

-- ────────────────────────────────────────────
-- 6. summaries
--   - 세션 종료 시 HCX-007(베이스)이 생성하는 3요소 요약
--   - [08. 기록 보기 / 0X. 상담 상세] 데이터 소스
--   - situation_text / emotion_text / thought_text : RAG 청크 원문 대응
--   - emotion_distribution : {"HURT":55,"ANXIOUS":32} 감정 비율 차트용
--   - quote_text   : [08] 카드 헤드라인 인용구
--   - memory_hint  : 다음 세션 시스템 프롬프트 블록③ recentSummary 주입용
--   - emotion_change: 프롬프트 recentSummary 압축 형식에 사용 ("불안 → 상처")
-- ────────────────────────────────────────────
CREATE TABLE summaries (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    session_id              VARCHAR(36)     NOT NULL UNIQUE,
    user_id                 BIGINT          NOT NULL,
    summary_text            TEXT            NOT NULL,
    situation_text          TEXT            NULL,
    emotion_text            TEXT            NULL,
    thought_text            TEXT            NULL,
    dominant_emotion        ENUM('HAPPY','SAD','ANGRY','ANXIOUS','HURT','EMBARRASSED') NULL,
    emotion_distribution    JSON            NULL,
    emotion_change          VARCHAR(255)    NULL,
    quote_text              VARCHAR(500)    NULL,
    memory_hint             TEXT            NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_summaries_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_summaries_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    INDEX idx_summaries_user_id (user_id),
    INDEX idx_summaries_created_at (created_at)
);

-- ────────────────────────────────────────────
-- 7. emotion_logs
--   - 감정 발생 이벤트 단위 적재 (감정 대시보드 / 통계용)
--   - source   : PRE_CHAT(06번) | MESSAGE(턴별) | SUMMARY(세션요약)
-- ────────────────────────────────────────────
CREATE TABLE emotion_logs (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    session_id  VARCHAR(36) NOT NULL,
    message_id  BIGINT      NULL,
    emotion_tag ENUM('HAPPY','SAD','ANGRY','ANXIOUS','HURT','EMBARRASSED') NOT NULL,
    source      ENUM('PRE_CHAT','MESSAGE','SUMMARY') NOT NULL,
    logged_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_emotion_logs_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_emotion_logs_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_emotion_logs_message
        FOREIGN KEY (message_id) REFERENCES chat_messages(id)
        ON DELETE SET NULL,
    INDEX idx_emotion_logs_user_id (user_id),
    INDEX idx_emotion_logs_session_id (session_id),
    INDEX idx_emotion_logs_source (source),
    INDEX idx_emotion_logs_logged_at (logged_at)
);

-- ────────────────────────────────────────────
-- 8. counseling_centers
--   - [09. 상담 센터 찾기] 마스터 데이터
--   - is_emergency : TRUE = 24시간 긴급(1393 등) → 화면 최상단 노출
--   - sort_order   : 목록 정렬 순서 (is_emergency 다음 기준)
--   - safety_events.resource_id가 이 테이블을 참조하므로 safety_events보다 먼저 정의한다.
-- ────────────────────────────────────────────
CREATE TABLE counseling_centers (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255)    NOT NULL,
    phone           VARCHAR(50)     NOT NULL,
    region          VARCHAR(100)    NULL,
    is_emergency    BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order      INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_counseling_centers_emergency (is_emergency, is_active),
    INDEX idx_counseling_centers_sort (sort_order),
    INDEX idx_counseling_centers_region (region)
);

-- ────────────────────────────────────────────
-- 9. safety_events
--   - [Safety 파트] 위험 감지 / 배너 / 평가 / 자원 안내 이력
--   - resource_id     : 클릭된 상담 자원 (CENTER_CALL_TAPPED 시 연결)
--   - forced_safety   : 일반 페르소나 응답 차단 후 강제 안전 발화 여부
-- ────────────────────────────────────────────
CREATE TABLE safety_events (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    session_id      VARCHAR(36) NOT NULL,
    message_id      BIGINT      NULL,
    resource_id     BIGINT      NULL,
    event_type      ENUM('RISK_DETECTED','BANNER_SHOWN',
                         'ASSESSMENT_OPENED','ASSESSMENT_COMPLETED',
                         'CENTER_LIST_VIEWED','CENTER_CALL_TAPPED',
                         'FORCED_SAFETY_REPLY') NOT NULL,
    risk_level      ENUM('MEDIUM','HIGH') NULL,
    forced_safety   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_safety_events_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_safety_events_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_safety_events_message
        FOREIGN KEY (message_id) REFERENCES chat_messages(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_safety_events_resource
        FOREIGN KEY (resource_id) REFERENCES counseling_centers(id)
        ON DELETE SET NULL,
    INDEX idx_safety_events_user_session (user_id, session_id),
    INDEX idx_safety_events_event_type (event_type),
    INDEX idx_safety_events_created_at (created_at)
);

-- ────────────────────────────────────────────
-- 10. self_assessments
--   - [0X. 안전 체크인] 자살 위험성 평가 결과
--   - assessment_type : 'SUICIDE_RISK' (추후 PHQ-9 등 확장 대비 VARCHAR)
--   - answers JSON    : {"q1":"...", "q2":"..."} 문항별 응답
-- ────────────────────────────────────────────
CREATE TABLE self_assessments (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    session_id      VARCHAR(36) NULL,
    assessment_type VARCHAR(50) NOT NULL DEFAULT 'SUICIDE_RISK',
    answers         JSON        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_self_assessments_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_self_assessments_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
        ON DELETE SET NULL,
    INDEX idx_self_assessments_user_id (user_id),
    INDEX idx_self_assessments_created_at (created_at)
    -- [정책] safety_events.risk_level = HIGH 조건 충족 시,
    --        자살 위험성 설문조사(SUICIDE_RISK) 실시 및 의료기관 안내는 강제 필수 조치임.
    --        score·risk_grade는 서버 로직에서 판정하며 DB에 저장하지 않는다.
);

-- ────────────────────────────────────────────
-- 11. rag_chunks
--   - 자체 RAG 파이프라인 인덱스 (세션 종료 시 비동기 적재)
--   - chunk_text  : 사용자 입력 수신 시 AI가 자동으로 핵심 키워드를 추출하여
--                   Vector Store 검색 쿼리로 사용하는 텍스트
--   - pulling_text: RAG 검색 결과로 실제 조회된 과거 채팅 요약문 전문
--                   (AI 응답 프롬프트 컨텍스트에 주입됨)
--   - vector_ref  : 외부 Vector Store(FAISS/ChromaDB) 내 문서 ID
-- ────────────────────────────────────────────
CREATE TABLE rag_chunks (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    session_id      VARCHAR(36) NOT NULL,
    summary_id      BIGINT      NULL,
    chunk_text      TEXT        NOT NULL,
    pulling_text    TEXT        NULL,
    vector_ref      VARCHAR(255) NULL,
    indexed_at      TIMESTAMP   NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_rag_chunks_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rag_chunks_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rag_chunks_summary
        FOREIGN KEY (summary_id) REFERENCES summaries(id)
        ON DELETE SET NULL,
    INDEX idx_rag_chunks_user_id (user_id),
    INDEX idx_rag_chunks_session_id (session_id),
    INDEX idx_rag_chunks_indexed_at (indexed_at)
);

-- ============================================================
-- ERD 관계 요약
--
-- users (1) ──── (1) profiles
-- users (1) ──── (1) user_settings
-- users (1) ──── (N) chat_sessions
-- users (1) ──── (N) summaries
-- users (1) ──── (N) emotion_logs
-- users (1) ──── (N) safety_events
-- users (1) ──── (N) self_assessments
-- users (1) ──── (N) rag_chunks
--
-- chat_sessions (1) ──── (N) chat_messages
-- chat_sessions (1) ──── (1) summaries
-- chat_sessions (1) ──── (N) emotion_logs
-- chat_sessions (1) ──── (N) safety_events
-- chat_sessions (0..1) ── (N) self_assessments
-- chat_sessions (1) ──── (N) rag_chunks
--
-- chat_messages (1) ──── (N) emotion_logs       [ON DELETE SET NULL]
-- chat_messages (1) ──── (N) safety_events      [ON DELETE SET NULL]
-- summaries     (1) ──── (N) rag_chunks         [ON DELETE SET NULL]
-- counseling_centers (1) ── (N) safety_events   [ON DELETE SET NULL]
--
-- ============================================================
-- 화면별 DB 조작 요약
--
-- [01 로그인]        users INSERT(신규) / UPDATE last_login_at(기존)
-- [02 약관동의]      users UPDATE terms_agreed_at, privacy_agreed_at
-- [03 온보딩 설문]   profiles INSERT (personal_instruction 포함),
--                    user_settings INSERT
-- [04 메인화면]      chat_sessions READ (deleted_at IS NULL),
--                    summaries.quote_text READ
-- [05 대화 선택]     chat_sessions INSERT (persona_type, status=ACTIVE)
-- [06 채팅전감정]    chat_sessions UPDATE pre_chat_emotion,
--                    emotion_logs INSERT (PRE_CHAT)
-- [07 채팅]          chat_messages INSERT × 2/턴 (ai_model 포함),
--                    emotion_logs INSERT (MESSAGE),
--                    safety_events INSERT (RISK_DETECTED, HIGH 시)
-- [채팅 요약]        chat_sessions UPDATE (ENDED/ABANDONED + ended_at
--                                        + summary_status=CREATED/SKIPPED),
--                    summaries INSERT, emotion_logs INSERT (SUMMARY),
--                    rag_chunks INSERT (비동기, chunk_text+pulling_text)
-- [위기 감지 배너]   safety_events INSERT (BANNER_SHOWN, FORCED_SAFETY_REPLY)
-- [안전 체크인]      self_assessments INSERT,
--                    safety_events INSERT (ASSESSMENT_OPENED / ASSESSMENT_COMPLETED)
-- [09 상담센터]      counseling_centers READ (is_active=TRUE ORDER BY sort_order),
--                    safety_events INSERT (CENTER_LIST_VIEWED / CENTER_CALL_TAPPED)
-- [08 기록보기]      chat_sessions READ (deleted_at IS NULL, status='ENDED'),
--                    summaries READ,
--                    chat_sessions UPDATE deleted_at=NOW() (삭제 스와이프)
-- [상담 상세]        summaries READ, chat_messages READ (optional)
-- [0X 프로필/설정]   profiles UPDATE (personal_instruction 재생성 포함),
--                    user_settings UPDATE
-- ============================================================
-- END OF SCHEMA v4.2 / Flyway V1
-- ============================================================
