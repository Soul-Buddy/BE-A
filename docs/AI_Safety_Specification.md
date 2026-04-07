# Soul Buddy — AI 안전 정책 명세서

*AI Safety Specification*

| **문서 버전** | **대상** | **원본 참조** | **주의** |
|---|---|---|---|
| v2.1 (2026-03-31) | BE-A 담당 (장지훈), SafetyFilter.java 구현 시 참조 | 05_safety_policy.md | 안전 정책은 기획·UX 결정보다 우선합니다. 임의로 완화하지 마세요. |

---

## 1. 위험도 레벨 정의

| **레벨** | **기준** | **처리** |
|---|---|---|
| LOW | 일반 고민, 스트레스, 일상 불안 | AI 일반 응답 반환 |
| MEDIUM | 지속적 우울감, 수면 장애, 무기력 언급 | AI 응답 + 전문 상담 권고 문구 추가 |
| HIGH | 자해·자살·극단적 선택 표현 감지 | AI 응답 차단, 안전 응답 고정 반환 |

---

## 2. 처리 흐름

| **단계** | **처리 주체** | **동작** |
|---|---|---|
| 1차 (preCheck) | SafetyFilter | HIGH 키워드 룰 기반 매칭 → HIGH 감지 시 LLM 호출 없이 즉시 안전 응답 반환 |
| 2차 (postProcess) | SafetyFilter | LLM 응답의 riskLevel 확인 → HIGH: 응답 교체 / MEDIUM: 권고 문구 추가 |

*ℹ️ 1차 필터에서는 MEDIUM 판정 없음 — MEDIUM은 LLM 2차 판정에 위임합니다.*

---

## 3. HIGH 위험 감지 키워드 (1차 룰 기반)

아래 키워드 포함 시 LLM 호출 없이 즉시 HIGH 처리합니다.

```java
private static final List<String> HIGH_RISK_KEYWORDS = List.of(
    "자살", "자해", "죽고 싶", "죽고싶", "사라지고 싶", "사라지고싶",
    "없어지고 싶", "없어지고싶", "극단적", "목숨", "유서",
    "스스로 목숨", "살기 싫", "살기싫", "죽는 게 낫", "죽는게 낫"
);
```

---

## 4. SafetyFilter 구현 명세

```java
public class SafetyFilter {

    // HIGH 키워드 1차 감지
    public RiskLevel preCheck(String userMessage) {
        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (userMessage.contains(keyword)) return RiskLevel.HIGH;
        }
        return RiskLevel.LOW;
    }

    // LLM 응답 후처리
    public ChatResponse postProcess(ChatResponse response) {
        if ("HIGH".equals(response.getRiskLevel())) {
            response.setAssistantMessage(SAFETY_MESSAGE);
            response.setRecommendedAction(SAFETY_ACTION);
        } else if ("MEDIUM".equals(response.getRiskLevel())) {
            response.setAssistantMessage(
                response.getAssistantMessage() + MEDIUM_RECOMMEND
            );
        }
        return response;
    }
}
```

---

## 5. 고정 안전 응답 문구

**5-1. HIGH — 안전 응답 (assistantMessage 전면 교체)**

```java
public static final String SAFETY_MESSAGE =
    "지금 많이 힘드시겠어요. 혼자 감당하기 어려운 순간엔 " +
    "전문가와 이야기 나눠보는 것이 큰 도움이 될 수 있어요. " +
    "정신건강 위기상담 전화 1577-0199는 24시간 운영됩니다. " +
    "언제든 연락해보실 수 있어요.";

public static final String SAFETY_ACTION =
    "정신건강 위기상담 전화 1577-0199 (24시간 운영)";
```

**5-2. MEDIUM — 권고 문구 (assistantMessage 뒤에 추가)**

```java
public static final String MEDIUM_RECOMMEND =
    "\n\n요즘 많이 힘드신 것 같아서, 전문 상담사와 이야기 나눠보시는 것도 " +
    "좋은 방법일 수 있어요. 혼자 감당하지 않아도 괜찮아요.";
```

---

## 6. 에러 코드

| **코드** | **상황** |
|---|---|
| AI_003 | 위험 감지 — 안전 응답 반환됨 |

---

## 7. 프론트엔드 처리 규칙

| **riskLevel** | **FE 처리** |
|---|---|
| HIGH | 안전 모달 즉시 표시 + 채팅 입력창 비활성화. 모달 내 recommendedAction 값 표시 + "확인" 버튼 |
| MEDIUM | 채팅 말풍선 하단에 권고 배너 표시 (닫기 가능) |
| LOW | 특별 처리 없음 |

---

## 8. 의료 비진단 원칙 (전체 팀 공통)

⚠️ UI 텍스트·에러 메시지·AI 응답 어디에서도 진단·병명 표현 금지

- "당신의 상태는 ~", "~증상이 있습니다" 표현 금지
- "정서 지원", "마음 돌봄", "이야기 나누기" 표현 사용

---

## 9. 감정 태깅 면책 사항

> `emotionTag`는 LLM이 대화 맥락에서 추론한 값이며, 동일 입력에 대해 결과가 달라질 수 있습니다.
> dashboard의 감정 통계(`emotionStats`)는 **참고용 감정 추이**로만 활용합니다.
> 정확한 감정 진단 데이터가 아닙니다.

**프론트엔드 표시 규칙:**
- 감정 차트/통계 영역에 "감정 추이 (참고용)" 문구를 함께 표시합니다.
- "진단", "분석 결과" 등의 표현을 사용하지 않습니다.

---

## 10. EmotionLog 저장 책임

- `EmotionLogService`는 `domain/dashboard/` 패키지에 위치합니다.
- `ChatService`가 AI 응답 저장 후 `EmotionLogService.save(userId, sessionId, emotionTag)`를 호출합니다.
- `AiChatService`는 DB를 직접 호출하지 않습니다.

---

*참조: 05_safety_policy.md | frontend-CLAUDE.md*

*문서 버전: v2.1 | 최종 수정: 2026-03-31*
