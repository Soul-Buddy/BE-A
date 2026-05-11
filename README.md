================================================================
Step 7: 백엔드 → 프론트엔드 요청사항 + FE 테스트 방법
작성일: 2026-05-11  (코드 직접 분석 기반 — 실제 DTO·Controller 확인)
최종 업데이트: 2026-05-11 (Step 10 최적화 반영 — Agreement 응답 형식 변경, 404 동작 변경)
기준: TestFolder/7-채팅 + BE-A/backend 동기화 완료 코드 (서버 테스트 통과)
================================================================


================================================================
1. BE 서버 실행 방법 (FE 테스트 전 필수)
================================================================

  위치: TestFolder/7-채팅/
  명령어:
    Windows: .\gradlew.bat bootRun
    Mac/Linux: ./gradlew bootRun

  서버 주소:  http://localhost:8080
  Swagger UI: http://localhost:8080/swagger-ui.html
  Swagger JSON: http://localhost:8080/api-docs

  필수 환경변수 (.env 또는 application-local.yml — 지훈에게 받을 것):
    DB_URL=jdbc:mysql://localhost:3306/soul_buddy
    DB_USERNAME=root
    DB_PASSWORD=...
    JWT_SECRET=...
    CLOVA_API_KEY=...
    GOOGLE_CLIENT_ID=...
    GOOGLE_CLIENT_SECRET=...

  주의:
    - MySQL 8 로컬 설치 + soul_buddy DB + v4.2 스키마 적용 필수
    - 스키마 파일: TestFolder/soul_buddy_schema_v4.2.sql
    - DB 없으면 서버 시작 불가 (ddl-auto: validate 모드)


================================================================
2. FE가 구현해야 할 사항 (우선순위 순)
================================================================

  ────────────────────────────────────────────────────────────
  [우선순위 1] API 공통 클라이언트 생성 (필수 선행 작업)
  ────────────────────────────────────────────────────────────

  파일: frontend/shared/lib/apiClient.ts (신규 생성)

  구현 예시:
    const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

    export async function apiFetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
      const token = localStorage.getItem("accessToken");
      const res = await fetch(`${BASE_URL}${endpoint}`, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          ...options?.headers,
        },
      });
      const json = await res.json();
      if (!json.success) throw new Error(json.error?.message ?? "API 오류");
      return json.data as T;
    }

  BE 공통 응답 포맷:
    성공: { "success": true,  "data": { ... },  "error": null }
    실패: { "success": false, "data": null, "error": { "code": "...", "message": "..." } }

  ★ [2026-05-11 수정] Agreement API (POST/GET /api/agreements)도 ApiResponse 래핑 적용됨.
     이전 버전에서는 래핑 없이 직접 반환했으나 수정됨. 다른 API와 동일하게 apiFetch 사용 가능.
     response.data.allAgreed 형태로 접근.

  ────────────────────────────────────────────────────────────
  [우선순위 2] 인증 흐름 구현
  ────────────────────────────────────────────────────────────

  파일: features/auth/google-login/ui/GoogleLoginButton.tsx

  현재 FE 상태: Google 로그인 후 NextAuth 세션만 생성, BE JWT 연동 없음
  필요한 변경:
    1. signIn("google") 실행 → NextAuth 세션의 id_token 추출
    2. POST /api/auth/login { "googleIdToken": session.idToken } 호출
    3. 응답 (ApiResponse<LoginResponse>):
       {
         "success": true,
         "data": {
           "accessToken": "eyJ...",
           "refreshToken": "eyJ...",
           "userId": 1,
           "isNewUser": false,
           "termsAgreed": true,
           "onboardingCompleted": false
         }
       }
    4. accessToken, refreshToken → localStorage 저장
    5. 화면 분기:
       - termsAgreed = false      → /onboarding/terms
       - onboardingCompleted = false → /onboarding/profile
       - 둘 다 true               → / (메인)

  NextAuth 콜백 수정 (shared/config/auth.ts):
    callbacks: {
      jwt({ token, account }) {
        if (account?.id_token) token.idToken = account.id_token;
        return token;
      },
      session({ session, token }) {
        session.idToken = token.idToken as string;
        return session;
      }
    }

  Token 갱신:
    POST /api/auth/refresh
    Body: { "refreshToken": "eyJ..." }
    응답: { "success": true, "data": { "accessToken": "eyJ..." } }

  ────────────────────────────────────────────────────────────
  [우선순위 3] 온보딩 폼 수정
  ────────────────────────────────────────────────────────────

  파일: features/onboarding/profile-form/model/store.ts

  현재 FE 필드 → BE 필드 변경 (실제 OnboardingRequest.java 기준):
    name (string)              → nickname (string)         [필수]
    age (string)               → age (number)              [선택] ← Number(age) 변환 필수
    gender "female"/"male"     → gender "FEMALE"/"MALE"    [선택] ← 대문자 변환 필수
    usagePurposes (string[])   → usageIntent (string)      [선택] ← 배열 join 또는 단일 선택
    (없음)                     → preferredTone (string)    [필수!] ← UI 추가 필수
    occupation (없으면 null)   → occupation (string)       [선택]
    hobbies (없으면 null)      → hobbies (string[])        [선택]
    likedThings (없으면 null)  → likedThings (string[])    [선택]
    dislikedThings (없으면 null)→ dislikedThings (string[]) [선택]

  API 호출 순서:
    ① POST /api/agreements  (약관 동의 화면)
       요청: { "termsAgreed": true, "privacyAgreed": true }
       응답 (★ ApiResponse 래핑 있음 — 2026-05-11 수정):
         { "success": true, "data": {
             "userId": 1, "email": "...", "nickname": "...",
             "termsAgreedAt": "...", "privacyAgreedAt": "...", "agreedAll": true
           }
         }
       접근: response.data.agreedAll

    ② POST /api/onboarding  (프로필 입력 화면)
       요청 예시:
         {
           "nickname": "홍길동",
           "age": 25,
           "gender": "FEMALE",
           "usageIntent": "감정 정리",
           "preferredTone": "FRIEND",
           "occupation": "학생",
           "hobbies": ["독서", "음악"],
           "likedThings": ["고양이"],
           "dislikedThings": ["소음"]
         }
       응답: { "success": true, "data": { "profileId": 1, "onboardingCompletedAt": "..." } }

  preferredTone UI (온보딩 폼 마지막 단계에 추가):
    "FRIEND"    → 포코 (친구형, 반말, 공감·환기)
    "COUNSELOR" → 루미 (상담사형, 존댓말, 성찰·구조적 응답)

  ────────────────────────────────────────────────────────────
  [우선순위 4] 채팅 API 연동
  ────────────────────────────────────────────────────────────

  ① 페르소나 목록 조회 (화면 진입 시, 인증 불필요)
     GET /api/personas
     응답: { "success": true, "data": [
       {
         "personaType": "FRIEND",
         "characterName": "포코",
         "displayName": "친구형",
         "description": "...",
         "tags": ["따뜻함", "공감"],
         "toneSample": "야, 오늘 어때?"
       },
       { "personaType": "COUNSELOR", "characterName": "루미", ... }
     ]}

  ② 세션 생성
     POST /api/sessions
     요청: { "personaType": "FRIEND" }
     응답: { "success": true, "data": {
       "sessionId": "uuid-string",
       "personaType": "FRIEND",
       "openingMessage": "안녕! 오늘 어떤 하루였어?",
       "recentSummary": null,
       "createdAt": "2026-05-11T..."
     }}
     → sessionId 로컬 상태에 저장 필수 (이후 모든 API에서 사용)

  ③ 채팅 전 감정 선택
     PATCH /api/sessions/{sessionId}/pre-chat-emotion
     요청: { "preChatEmotion": "ANXIOUS" }
     감정 6종: HAPPY | SAD | ANGRY | ANXIOUS | HURT | EMBARRASSED
     응답: { "success": true, "data": { "sessionId": "...", "preChatEmotion": "ANXIOUS", ... } }

  ④ 채팅 메시지 전송 (미구현 — 헌영 작업 중)
     POST /api/chat
     요청:
       {
         "sessionId": "uuid",
         "personaType": "FRIEND",
         "message": "오늘 너무 힘들었어",
         "recentSummary": null
       }
     예상 응답: { "success": true, "data": {
       "assistantMessage": "야, 무슨 일이 있었어?",
       "emotionTag": "HURT",
       "riskLevel": "LOW",
       "interventionType": "sympathy_support",
       "ragUsed": false,
       "aiModel": "HCX-005-FRIEND",
       "summary": null,
       "memoryHint": null,
       "recommendedAction": null
     }}
     ※ 이 엔드포인트는 현재 BE 미구현. Swagger에 없음. 헌영이 추가 예정.

  ⑤ 대화 히스토리 조회 (실제 ChatHistoryResponse.java 기준)
     GET /api/chat/history/{sessionId}?page=0&size=50
     응답: { "success": true, "data": {
       "sessionId": "uuid",
       "messages": [
         {
           "messageId": 1,
           "sender": "USER",          ← "USER" | "ASSISTANT" | "SYSTEM"
           "content": "오늘 너무 힘들었어",
           "emotionTag": "HURT",       ← nullable
           "riskLevel": "LOW",         ← nullable
           "interventionType": null,   ← nullable
           "ragUsed": false,
           "aiModel": null,            ← nullable (USER 메시지는 null)
           "createdAt": "2026-05-11T..."
         }
       ],
       "totalCount": 10,
       "page": 0,
       "size": 50
     }}

     ★ FE 현재 필드 → BE 필드 매핑:
       role "buddy"  → sender "ASSISTANT"
       role "user"   → sender "USER"
       text          → content
       id (string)   → messageId (number)

  ⑥ 세션 종료 (AI 요약 생성)
     PATCH /api/sessions/{sessionId}/end
     응답: { "success": true, "data": {
       "sessionId": "...",
       "status": "ENDED",
       "summaryStatus": "COMPLETED",
       "summaryText": "오늘 많이 힘들었던 하루...",
       "situationText": "과제 마감과 팀 갈등이 겹쳤음",
       "emotionText": "불안과 피로감이 지배적이었음",
       "thoughtText": "내가 너무 부족한 건 아닐까 걱정했음",
       "dominantEmotion": "ANXIOUS",
       "emotionChange": "불안 → 안정",
       "memoryHint": null,
       "endedAt": "2026-05-11T..."
     }}

  ────────────────────────────────────────────────────────────
  [우선순위 5] 감정 필드 매핑
  ────────────────────────────────────────────────────────────
**저희가 대화 시작 전 감정 9종 선택하기로 했는데 그거랑 별개로 해당 대화 채팅 중에는 감정 6종으로 분류됩니다. 
그래서 대화 채팅 시작 전 감정 선택 페에지는 감정 9종, 채팅 중에 감정 분류는 6종으로 한다고 생각하시면 됩니다. 
그리고 대화 전 감정 선택은 AI 프롬프트로는 안 넣을 것 같고 그냥 보여주기식으로 선택하는거라고 생각하면 됩니다. 만약에 대화 시작 전 감정도 AI프롬프트로 넣고 싶으면 말해주세요.
  BE EmotionTag 6종 (확정):
    HAPPY | SAD | ANGRY | ANXIOUS | HURT | EMBARRASSED

  FE 현재 9종 → BE 6종 매핑:
    tired (피곤함)       
    anxious (불안함)      
    lethargic (무기력)    
    comfortable (편안함)  
    excited (설렘)        
    lonely (외로움)       
    frustrated (답답함)   
    hopeful (희망)      
    angry (화남)          

  PATCH /api/sessions/{id}/pre-chat-emotion 전송 시:
    현재 FE: { emotionId: "anxious" }
    변경 후:  { preChatEmotion: "ANXIOUS" }

  ────────────────────────────────────────────────────────────
  [우선순위 6] 세션·대화목록 연동
  ────────────────────────────────────────────────────────────

  GET /api/sessions?page=0&size=20
  (선택 쿼리파람: status=ACTIVE | ENDED)

  응답 items 필드 (실제 SessionItemResponse.java 기준):
    sessionId      → 세션 UUID
    personaType    → "FRIEND" | "COUNSELOR"
    characterName  → "포코" | "루미"  (FE 하드코딩 대체 가능)
    status         → "ACTIVE" | "ENDED"
    preChatEmotion → EmotionTag | null
    summaryStatus  → "PENDING" | "COMPLETED" | "FAILED"
    quoteText      → 요약 대표 문장 (ENDED 상태일 때 존재)
    dominantEmotion → EmotionTag | null
    emotionChange  → "불안 → 안정" 형태 문자열 | null
    startedAt      → ISO DateTime
    endedAt        → ISO DateTime | null

  FE 필드 매핑:
    FE id           → sessionId
    FE buddyId "poco"/"lumi" → personaType "FRIEND"/"COUNSELOR"
    FE dateLabel    → startedAt (FE에서 포맷)
    FE dotColor     → BE 없음 (FE 자체 로직 유지)
    FE emotionFromLabel → preChatEmotion
    FE title        → quoteText
    FE emotionToLabel → emotionChange

  ────────────────────────────────────────────────────────────
  [우선순위 7] 대시보드 연동
  ────────────────────────────────────────────────────────────

  GET /api/dashboard/me?page=0&size=10
  응답 (실제 DashboardResponse.java 기준):
    {
      "userId": 1,
      "emotionStats": {
        "HAPPY": 3,
        "SAD": 1,
        "ANXIOUS": 5
      },
      "recentSummaries": [
        {
          "sessionId": "uuid",
          "date": "2026-05-11",
          "personaType": "FRIEND",
          "characterName": "포코",
          "quoteText": "오늘 힘들었던 것들을 잘 이겨냈어",
          "dominantEmotion": "ANXIOUS",
          "emotionChange": "불안 → 안정"
        }
      ],
      "totalSummaryCount": 12,
      "page": 0,
      "size": 10
    }

  ────────────────────────────────────────────────────────────
  [우선순위 8] 사용자·프로필 연동
  ────────────────────────────────────────────────────────────

  GET /api/users/me
  응답: { "success": true, "data":
    { "userId": 1, "nickname": "홍길동", "email": "...",
      "termsAgreed": true, "onboardingCompleted": true } }

  GET /api/profiles/me
  응답: { "success": true, "data":
    { "profileId": 1, "userId": 1, "nickname": "홍길동",
      "age": 25, "gender": "FEMALE", "occupation": "학생",
      "usageIntent": "감정 정리", "hobbies": ["독서"],
      "preferredTone": "FRIEND",
      "likedThings": ["고양이"], "dislikedThings": ["소음"] } }

  PUT /api/profiles/me
  요청: 온보딩과 동일한 필드 (nickname, age, gender, preferredTone 등)

  GET /api/users/settings
  PUT /api/users/settings
  요청: { "pushEnabled": true, "emailEnabled": false, ... }

  ────────────────────────────────────────────────────────────
  [우선순위 9] 미들웨어 활성화
  ────────────────────────────────────────────────────────────

  파일: frontend/proxy.ts (현재 주석 처리)
  → 주석 해제하여 미인증 사용자 /login 리다이렉트 활성화


================================================================
3. BE 완성 API 목록 (현재 기준)
================================================================

  ✅ POST   /api/auth/login                       Google 로그인 → JWT 발급
  ✅ POST   /api/auth/refresh                     AccessToken 갱신
  ✅ POST   /api/agreements                       약관 동의 제출
  ✅ GET    /api/agreements                       약관 동의 상태 조회
  ✅ POST   /api/onboarding                       온보딩 프로필 저장
  ✅ GET    /api/profiles/me                      내 프로필 조회
  ✅ PUT    /api/profiles/me                      프로필 수정
  ✅ GET    /api/users/me                         내 정보 조회
  ✅ GET    /api/users/settings                   알림 설정 조회
  ✅ PUT    /api/users/settings                   알림 설정 변경
  ✅ GET    /api/personas                         페르소나 목록 (포코/루미)
  ✅ POST   /api/sessions                         세션 생성 + AI 오프닝 메시지
  ✅ GET    /api/sessions                         세션 목록
  ✅ DELETE /api/sessions/{sessionId}             세션 삭제
  ✅ PATCH  /api/sessions/{sessionId}/end         세션 종료 + AI 요약
  ✅ PATCH  /api/sessions/{sessionId}/pre-chat-emotion  채팅 전 감정 선택
  ✅ GET    /api/chat/history/{sessionId}         대화 히스토리 조회
  ✅ GET    /api/dashboard/me                     대시보드 (감정통계 + 요약카드)
  ✅ GET    /api/health                           서버 상태 확인

  ⏳ POST   /api/chat                            AI 채팅 메시지 전송 (헌영 작업 중)
  ⏳ GET    /api/counseling-centers              상담센터 목록 (준영 작업 예정)


================================================================
4. FE 테스트 방법
================================================================

  ────────────────────────────────────────────────────────────
  [테스트 1] Swagger UI로 API 직접 호출
  ────────────────────────────────────────────────────────────

  1. BE 서버 실행 (.\gradlew.bat bootRun)
  2. http://localhost:8080/swagger-ui.html 접속
  3. 우측 상단 "Authorize" → BearerAuth에 accessToken 입력
  4. 각 엔드포인트 "Try it out" → Execute → 응답 확인

  인증 없이 테스트 가능한 엔드포인트:
    GET  /api/health
    POST /api/auth/login   (Google ID Token 필요)
    GET  /api/personas

  ────────────────────────────────────────────────────────────
  [테스트 2] curl로 직접 API 호출
  ────────────────────────────────────────────────────────────

  # 서버 상태 확인
  curl http://localhost:8080/api/health

  # 페르소나 목록 (인증 불필요)
  curl http://localhost:8080/api/personas

  # 로그인 (실제 Google ID Token 필요)
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"googleIdToken\": \"eyJ...\"}"

  # 인증 필요한 API (accessToken 교체)
  curl http://localhost:8080/api/users/me \
    -H "Authorization: Bearer {accessToken}"

  curl http://localhost:8080/api/sessions \
    -H "Authorization: Bearer {accessToken}"

  curl -X POST http://localhost:8080/api/sessions \
    -H "Authorization: Bearer {accessToken}" \
    -H "Content-Type: application/json" \
    -d "{\"personaType\": \"FRIEND\"}"

  ────────────────────────────────────────────────────────────
  [테스트 3] AI 파이프라인 단독 테스트 (인증 불필요)
  ────────────────────────────────────────────────────────────

  AI 채팅 응답 테스트:
  curl -X POST http://localhost:8080/api/dev/ai-test/chat \
    -H "Content-Type: application/json" \
    -d "{
      \"request\": {
        \"sessionId\": \"test-session-001\",
        \"personaType\": \"FRIEND\",
        \"message\": \"오늘 너무 힘들었어\"
      },
      \"context\": {
        \"personaType\": \"FRIEND\",
        \"nickname\": \"지훈\",
        \"personalInstruction\": \"닉네임: 지훈\",
        \"recentTurns\": [],
        \"firstTurn\": true
      },
      \"recentHighCount\": 0
    }"

  AI 오프닝 메시지 테스트:
  curl -X POST http://localhost:8080/api/dev/ai-test/opening \
    -H "Content-Type: application/json" \
    -d "{
      \"personaType\": \"FRIEND\",
      \"nickname\": \"지훈\",
      \"visitState\": \"FIRST\"
    }"

  AI 요약 테스트:
  curl -X POST http://localhost:8080/api/dev/ai-test/summarize \
    -H "Content-Type: application/json" \
    -d "{
      \"sessionId\": \"test-session-001\",
      \"userId\": 1,
      \"messages\": [
        {\"sender\": \"USER\", \"content\": \"오늘 너무 힘들었어\", \"createdAt\": \"2026-05-11T10:00:00\"},
        {\"sender\": \"ASSISTANT\", \"content\": \"어떤 일이 있었어?\", \"createdAt\": \"2026-05-11T10:00:05\"}
      ]
    }"

  ────────────────────────────────────────────────────────────
  [테스트 4] FE 코드에서 실제 API 연동 흐름 테스트
  ────────────────────────────────────────────────────────────

  1단계: .env.local 설정
    AUTH_SECRET=           (npx auth secret 으로 생성)
    AUTH_GOOGLE_ID=        (Google Cloud Console → OAuth 2.0 클라이언트 ID)
    AUTH_GOOGLE_SECRET=
    NEXT_PUBLIC_API_URL=http://localhost:8080

  2단계: apiClient.ts 생성 후 간단한 테스트
    // 테스트용 코드 (아무 컴포넌트에서 실행)
    const personas = await apiFetch<PersonaResponse[]>("/api/personas");
    console.log(personas);

  3단계: Google 로그인 → JWT 발급 흐름 테스트
    로그인 버튼 클릭 → Google 계정 선택 → idToken 추출
    → POST /api/auth/login → accessToken을 localStorage에 저장
    → GET /api/users/me 호출해서 내 정보 확인

  4단계: 채팅 흐름 테스트 (POST /api/chat 구현 후)
    1. POST /api/sessions → sessionId 획득
    2. PATCH /api/sessions/{id}/pre-chat-emotion
    3. POST /api/chat (메시지 전송)
    4. GET /api/chat/history/{sessionId}
    5. PATCH /api/sessions/{id}/end

  ────────────────────────────────────────────────────────────
  [테스트 5] 에러 케이스 처리 확인
  ────────────────────────────────────────────────────────────

  401 Unauthorized: accessToken 없거나 만료
    → POST /api/auth/refresh 로 재발급 후 재시도

  403 Forbidden: 다른 사용자의 세션 접근
    → 에러 코드 AUTH_002

  404 Not Found: 없는 세션 ID
    → 에러 응답: { "success": false, "data": null,
                  "error": { "code": "SESSION_001", "message": "세션을 찾을 수 없습니다." } }

  ★ [2026-05-11 수정] 존재하지 않는 URL 호출 시 → 500 아닌 404 반환으로 변경됨
    예: GET /api/dev/nonexistent
    응답: { "success": false, "error": { "code": "COMMON_001", "message": "요청한 리소스를 찾을 수 없습니다." } }
    FE에서 404 응답도 핸들링 필요.

  CORS 오류 시:
    → BE CorsConfig.java에 http://localhost:3000 허용 확인


================================================================
5. 성환 - 코드 받은 후 BE 서버 기동·테스트 절차 (2026-05-11 기준)
================================================================

  ────────────────────────────────────────────────────────────
  Step 1. 코드 수령 및 .env 파일 확인 (필수)
  ────────────────────────────────────────────────────────────

  BE 코드 위치: dev 브랜치 병합 후 backend/ 폴더
  서버 기동 전 반드시 backend/ 폴더 안에 .env 파일이 있어야 함.

  .env 파일 없으면 기동 시 아래 에러 발생:
    Could not resolve placeholder 'CLOVA_API_KEY'

  .env 파일은 git에 올라가지 않으므로 지훈에게 직접 받을 것.
  파일 위치: backend/.env  (backend 폴더 바로 안, src 아님)

  .env 필수 항목:
    DB_URL=jdbc:mysql://localhost:3306/soul_buddy?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    DB_USERNAME=root
    DB_PASSWORD=(지훈에게 확인)
    JWT_SECRET=(지훈에게 확인)
    CLOVA_API_KEY=(지훈에게 확인)
    GOOGLE_CLIENT_ID=(지훈에게 확인)
    GOOGLE_CLIENT_SECRET=(지훈에게 확인)
    SERVER_PORT=8080
    CORS_ORIGINS=http://localhost:3000

  ────────────────────────────────────────────────────────────
  Step 2. MySQL DB 준비
  ────────────────────────────────────────────────────────────

  MySQL 8 로컬 설치 후:
    CREATE DATABASE soul_buddy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  스키마 적용:
    mysql -u root -p soul_buddy < TestFolder/soul_buddy_schema_v4.2.sql
    (또는 로컬 개발 시 application-local.yml의 ddl-auto: update로 자동 생성)

  ────────────────────────────────────────────────────────────
  Step 3. 서버 기동
  ────────────────────────────────────────────────────────────

  ※ 주의: 코드 동기화(파일 교체) 후에는 반드시 clean 먼저 실행.
     파일만 교체하고 bootRun하면 구버전 코드로 동작할 수 있음.

  Windows:
    cd backend
    .\gradlew.bat clean bootRun

  Mac/Linux:
    cd backend
    ./gradlew clean bootRun

  정상 기동 확인 로그:
    Tomcat started on port 8080
    Started SoulBuddyApplication in XX seconds

  ────────────────────────────────────────────────────────────
  Step 4. 서버 기동 확인 테스트 (인증 불필요)
  ────────────────────────────────────────────────────────────

  아래 명령어로 서버 상태 및 AI 파이프라인 확인:

  # 1. 서버 상태
  curl http://localhost:8080/api/health
  예상: {"success":true,"data":{"status":"UP","timestamp":"..."}}

  # 2. Swagger API 목록 (21개 경로 확인)
  curl http://localhost:8080/api-docs | python -c "import sys,json; d=json.load(sys.stdin); print('paths:', len(d['paths']))"
  예상: paths: 21

  # 3. AI 오프닝 메시지 (포코)
  curl -X POST http://localhost:8080/api/dev/ai-test/opening ^
    -H "Content-Type: application/json" ^
    -d "{\"personaType\":\"FRIEND\",\"nickname\":\"Tester\",\"visitState\":\"FIRST\"}"
  예상: {"success":true,"data":{"openingMessage":"...포코 인사말...","aiModel":"HCX-005-FRIEND"}}

  # 4. AI 채팅 응답 (일반 감정)
  curl -X POST http://localhost:8080/api/dev/ai-test/chat ^
    -H "Content-Type: application/json" ^
    -d "{\"request\":{\"sessionId\":\"test-001\",\"personaType\":\"FRIEND\",\"message\":\"I feel tired\"},\"context\":{\"personaType\":\"FRIEND\",\"firstTurn\":false},\"recentHighCount\":0}"
  예상: success:true, riskLevel:LOW, emotionTag 중 하나, forcedSafety:false

  # 5. Safety HIGH 감지 (PowerShell에서 실행 — cmd/bash에서는 한글 인코딩 문제)
  $bodyJson = '{"request":{"sessionId":"test-002","personaType":"FRIEND","message":"죽고 싶어"},"context":{"personaType":"FRIEND","firstTurn":false},"recentHighCount":0}'
  $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyJson)
  $response = Invoke-WebRequest -Uri "http://localhost:8080/api/dev/ai-test/chat" -Method POST -Body $bodyBytes -ContentType "application/json; charset=utf-8"
  $response.Content | ConvertFrom-Json | Select-Object -ExpandProperty data | Select riskLevel, forcedSafety, aiModel
  예상: riskLevel=HIGH, forcedSafety=True, aiModel=SYSTEM

  # 6. 없는 URL 호출 → 404 확인
  curl -w "\nHTTP:%{http_code}" http://localhost:8080/api/dev/nonexistent
  예상: HTTP:404, {"success":false,"error":{"code":"COMMON_001",...}}

  ────────────────────────────────────────────────────────────
  Step 5. Swagger UI로 인증 필요 API 테스트
  ────────────────────────────────────────────────────────────

  1. http://localhost:8080/swagger-ui.html 접속
  2. POST /api/auth/login 으로 Google ID Token 입력 → accessToken 발급
  3. 우측 상단 "Authorize" → BearerAuth 에 accessToken 붙여넣기
  4. 이후 인증 필요한 API (GET /api/users/me 등) "Try it out" 테스트

  ────────────────────────────────────────────────────────────
  한글 전송 주의사항 (테스트 도구별)
  ────────────────────────────────────────────────────────────

  PowerShell (권장):
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyJson)
    Invoke-WebRequest -Body $bodyBytes -ContentType "application/json; charset=utf-8"

  Bash/cmd curl (한글 포함 시 주의):
    한글 포함 Body는 인코딩 문제로 서버 파싱 실패 가능.
    영문 닉네임 사용하거나 Postman/PowerShell 사용 권장.

  Postman: UTF-8 기본 지원 → 한글 포함 테스트 무관하게 정상 동작.

  FE(Next.js fetch): UTF-8 기본 → 한글 정상 동작.


================================================================
6. 필드명 주요 변경사항 (FE 수정 체크리스트 — 2026-05-11 최종)
================================================================

  [버디/페르소나]
  ☐ FE 하드코딩 "poco"/"lumi" → GET /api/personas 응답으로 교체
  ☐ FE buddyId "poco"/"lumi" → personaType "FRIEND"/"COUNSELOR"

  [감정]
  ☐ 9종 → 6종 변경 (HAPPY/SAD/ANGRY/ANXIOUS/HURT/EMBARRASSED)
    OR 9종 유지 + 전송 시 6종으로 매핑
  ☐ emotionId → preChatEmotion (PATCH /api/sessions/.../pre-chat-emotion)

  [프로필/온보딩]
  ☐ name → nickname
  ☐ age: string → age: number  (Number() 변환)
  ☐ gender "female"/"male" → "FEMALE"/"MALE"
  ☐ usagePurposes (배열) → usageIntent (문자열)
  ☐ preferredTone UI 추가 필수 (현재 없음)

  [메시지]
  ☐ role "buddy" → sender "ASSISTANT"
  ☐ role "user"  → sender "USER"
  ☐ text → content
  ☐ id (string) → messageId (number)

  [세션]
  ☐ id → sessionId
  ☐ buddyId "poco"/"lumi" → personaType "FRIEND"/"COUNSELOR"
  ☐ title → quoteText
  ☐ dateLabel → startedAt (FE에서 날짜 포맷)
  ☐ emotionFromLabel → preChatEmotion
  ☐ emotionToLabel → emotionChange

  [대시보드]
  ☐ emotionStats: Map<EmotionTag, Long> 형태로 파싱
    예: { "HAPPY": 3, "ANXIOUS": 5 }

  [신규 파일 생성]
  ☐ shared/lib/apiClient.ts  (JWT 헤더 자동 주입)
  ☐ proxy.ts 주석 해제 (미들웨어 활성화)


================================================================
7. FE(성환)에 전달할 파일 목록
================================================================

  지금 전달 가능:
    TestFolder/swagger/API_swagger.json          ← 전체 API 스펙 (현재 코드 기준)
    TestFolder/docs/01_api_contract.md           ← API 계약서
    new_update_by_date/20260511_Step7_...txt     ← 이 파일 (BE→FE 요청사항 + 테스트 방법)
    new_update_by_date/20260511_Step10_...txt    ← BE 최적화 변경사항 (Agreement 형식 변경 포함)
    .env 파일 (지훈에게 직접 전달 — git 업로드 금지)

  POST /api/chat 완성 후 추가 전달:
    API_swagger.json 업데이트 버전 (헌영 완성 후 ./gradlew bootRun → /api-docs 재추출)


================================================================
END
================================================================
