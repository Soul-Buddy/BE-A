# frontend/CLAUDE.md — 프론트엔드 AI 코딩 지침

> 이 파일은 프론트엔드 코드 작성 시 Claude가 읽어야 할 컨텍스트입니다.
> 루트 `CLAUDE.md`와 함께 읽으세요.

---

## 기술 스택

- **Next.js 14** (App Router)
- **TypeScript**
- **Tailwind CSS**
- **PWA** (`next-pwa` 또는 `@ducanh2912/next-pwa`)
- **상태관리**: Zustand 또는 React Query (팀 결정)
- **HTTP**: Axios 또는 fetch with wrapper

---

## 디렉토리 구조

```
frontend/
├── app/
│   ├── layout.tsx
│   ├── page.tsx                    // 랜딩 또는 로그인 리다이렉트
│   ├── (auth)/
│   │   └── login/page.tsx          // 소셜 로그인 페이지
│   ├── onboarding/
│   │   └── page.tsx                // 온보딩 설문 (최초 1회)
│   ├── dashboard/
│   │   └── page.tsx                // 감정 로그 + 요약 대시보드
│   ├── chat/
│   │   ├── page.tsx                // 페르소나 선택 + 세션 시작
│   │   └── [sessionId]/
│   │       └── page.tsx            // 실제 채팅 UI
│   └── api/                        // Next.js API Routes (필요 시)
│
├── components/
│   ├── ui/                         // 공용 UI 컴포넌트
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Modal.tsx               // 위험 상황 안전 모달 포함
│   │   └── EmotionBadge.tsx
│   ├── chat/
│   │   ├── ChatBubble.tsx          // USER / ASSISTANT 말풍선
│   │   ├── ChatInput.tsx           // 메시지 입력창
│   │   └── RiskBanner.tsx          // MEDIUM 위험도 배너
│   ├── dashboard/
│   │   ├── EmotionChart.tsx        // 감정 변화 차트
│   │   └── SummaryCard.tsx         // 세션 요약 카드
│   └── persona/
│       └── PersonaSelector.tsx     // 페르소나 선택 UI
│
├── lib/
│   ├── api/
│   │   ├── client.ts               // Axios 인스턴스 + 인터셉터
│   │   ├── auth.ts                 // 인증 API 함수
│   │   ├── chat.ts                 // 채팅 API 함수
│   │   ├── profile.ts              // 프로필 API 함수
│   │   └── dashboard.ts            // 대시보드 API 함수
│   └── utils/
│       └── risk.ts                 // riskLevel 처리 유틸
│
├── types/
│   ├── api.ts                      // 공통 ApiResponse<T> 타입
│   ├── chat.ts                     // ChatRequest, ChatResponse 타입
│   ├── profile.ts                  // Profile, OnboardingRequest 타입
│   └── persona.ts                  // PersonaType enum
│
└── public/
    ├── manifest.json               // PWA manifest
    └── icons/
```

---

## 타입 정의 (반드시 준수)

```typescript
// types/api.ts
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
}

// types/chat.ts
export interface ChatRequest {
  userId: number;
  sessionId: string;
  personaType: 'FRIEND' | 'COUNSELOR' | 'EMPATHY';
  message: string;
  recentSummary?: string | null;
}

export interface ChatResponse {
  assistantMessage: string;
  emotionTag: 'ANXIOUS' | 'SAD' | 'CALM' | 'HAPPY' | 'NEUTRAL' | 'ANGRY';
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  summary: string | null;
  memoryHint: string | null;
  recommendedAction: string | null;
}
```

---

## 위험도 처리 규칙 (필수 구현)

```typescript
// lib/utils/risk.ts
export function handleRiskLevel(
  riskLevel: string,
  recommendedAction: string | null,
  showSafetyModal: (action: string) => void,
  showMediumBanner: () => void
) {
  if (riskLevel === 'HIGH') {
    showSafetyModal(recommendedAction ?? '정신건강 위기상담 전화 1577-0199');
    // 입력창 비활성화
  } else if (riskLevel === 'MEDIUM') {
    showMediumBanner();
  }
}
```

---

## API Base URL 설정

```typescript
// lib/api/client.ts
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
```

환경변수: `NEXT_PUBLIC_API_URL` (`.env.local`에 설정)

---

*문서 버전: v1.0 | 최종 수정: 2026-03-14*
