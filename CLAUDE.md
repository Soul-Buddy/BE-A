# CLAUDE.md — Soul Buddy 프로젝트 AI 코딩 마스터 지침

> 이 파일은 Claude(AI)가 Soul Buddy 프로젝트 코드를 작성할 때 가장 먼저 읽어야 할 파일입니다.
> 프로젝트 루트에 위치하며, 모든 컨텍스트의 출발점입니다.

---

## 1. 프로젝트 한 줄 정의

**Soul Buddy**는 사용자의 성향·상황·선호 말투를 반영하여 개인화된 AI 대화, 대화 요약,
감정 기록을 제공하는 **정서 지원 웹/PWA 서비스**입니다.
의료 진단이 아닌 정서 지원·자기 성찰 보조 서비스임을 항상 기억하세요.

---

## 2. 기술 스택 (절대 변경 금지)

| 영역 | 스택 | 비고 |
|------|------|------|
| Frontend | Next.js 14 (App Router) + TypeScript | PWA 지원 |
| Backend | Java 17 + Spring Boot 3.x + Gradle | 단일 서버 |
| Database | MySQL 8 | Spring Data JPA |
| AI | OpenAI API (GPT-4o) | **확정 — 다른 LLM 사용 금지** |
| 배포 | Frontend → Vercel / Backend → Render or Railway | |
| 문서화 | Swagger (OpenAPI 3.0) | |

---

## 3. 레포지토리 구조

```
soul-buddy/
├── CLAUDE.md                  ← 지금 이 파일 (AI 코딩 시 가장 먼저 읽기)
├── docs/
│   ├── 00_project_overview.md     ← 프로젝트 전체 개요 + MVP 범위
│   ├── 01_api_contract.md         ← 프론트↔백엔드 API 계약 (필드명·타입·예시)
│   ├── 02_db_schema.md            ← DB 테이블 구조 + ERD 설명
│   ├── 03_ai_spec.md              ← AI 입출력 스펙 + 프롬프트 규칙
│   ├── 04_persona_prompts.md      ← 페르소나별 시스템 프롬프트 전문
│   └── 05_safety_policy.md        ← 위험 감지 정책 + 안전 응답 규칙
├── frontend/                  ← Next.js 프로젝트
│   └── ... (frontend/CLAUDE.md 참조)
└── backend/                   ← Spring Boot 프로젝트
    └── ... (backend/CLAUDE.md 참조)
```

---

## 4. AI가 코드 작성 시 반드시 지킬 규칙

### 4-1. 절대 금지
- AI 응답에 진단·처방·의학적 판정 표현 사용 금지
- API Key, DB 비밀번호 등 민감정보 코드 하드코딩 금지
- `riskLevel = HIGH` 상황에서 일반 응답 반환 금지 (반드시 안전 응답 우선)
- 프론트-백엔드 간 필드명 임의 변경 금지 (→ `docs/01_api_contract.md` 준수)
- OpenAI GPT-4o 외 다른 LLM API 코드 작성 금지

### 4-2. 응답 구조 고정
백엔드가 프론트로 반환하는 **모든 API 응답**은 아래 공통 포맷을 사용합니다:
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```
실패 시:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "AI_001",
    "message": "AI 응답 생성에 실패했습니다."
  }
}
```

### 4-3. ChatResponse 구조 고정
AI 채팅 응답은 항상 아래 필드를 포함해야 합니다:
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

### 4-4. 인증 및 사용자 식별
- **userId는 Request Body에서 받지 않습니다.** JWT 토큰에서 추출합니다.
- `SecurityContext`에서 인증된 사용자 ID를 꺼내 사용하세요.
- Body에 userId가 있더라도 무시하고 JWT 기준으로 처리합니다.

### 4-5. 환경변수
코드에서 외부 값이 필요할 때는 반드시 환경변수로 참조합니다:
- `OPENAI_API_KEY`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `CORS_ORIGIN`, `SERVER_PORT`

---

## 5. 브랜치 전략

```
main       ← 배포용 (직접 push 금지)
dev        ← 통합 브랜치
feature/기능명   ← 개발 브랜치 (예: feature/ai-chat, feature/auth)
```

---

## 6. 각 docs 파일 요약

| 파일 | 언제 읽어야 하나 |
|------|----------------|
| `docs/00_project_overview.md` | 프로젝트 처음 시작할 때, MVP 범위 확인할 때 |
| `docs/01_api_contract.md` | API 엔드포인트 구현/호출 시 |
| `docs/02_db_schema.md` | 엔티티·리포지토리 작성 시 |
| `docs/03_ai_spec.md` | AI 연동 코드 작성 시 |
| `docs/04_persona_prompts.md` | 프롬프트 빌더 구현 시 |
| `docs/05_safety_policy.md` | 위험 감지·안전 응답 로직 작성 시 |

---

*문서 버전: v1.2 | 최종 수정: 2026-03-31*
