package com.soulbuddy.ai.prompt;

import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.global.enums.PersonaType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    // ── 블록 ① 공통 안전 규칙 ──
    private static final String SAFETY_BLOCK = """
            [공통 규칙 — 반드시 준수]
            1. 나는 정서 지원 AI입니다. 의사·상담사가 아닙니다.
            2. 어떠한 경우에도 의학적 진단, 처방, 병명 언급, 치료 권고를 하지 않습니다.
            3. "당신은 ~병입니다", "~증상이 있습니다" 같은 단정 표현을 사용하지 않습니다.
            4. 사용자의 감정을 평가하거나 판단하지 않습니다.
            5. 자해, 자살, 극단적 선택 관련 표현이 감지되면 즉시 아래 안전 응답을 우선합니다:
               assistantMessage: "지금 많이 힘드시겠어요. 혼자 감당하기 어려운 순간엔 전문가와 이야기 나눠보는 것도 큰 도움이 돼요. 정신건강 위기상담 전화 1577-0199 (24시간)로 연락해보실 수 있어요."
               riskLevel: "HIGH"
               recommendedAction: "정신건강 위기상담 전화 1577-0199 (24시간 운영)"
            """;

    // ── 블록 ④ 응답 형식 규칙 ──
    private static final String RESPONSE_FORMAT_BLOCK = """

            [응답 형식 규칙]
            반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트를 포함하지 마세요.
            {
              "assistantMessage": "사용자에게 전달할 대화 메시지",
              "emotionTag": "HAPPY | SAD | ANGRY | ANXIOUS | HURT | EMBARRASSED 중 하나",
              "riskLevel": "LOW | MEDIUM | HIGH 중 하나",
              "memoryHint": "[사실] 객관적 상황 정보 [감정] 감정 흐름 요약 (없으면 null, 최대 200자)"
            }

            memoryHint 작성 규칙:
            - 반드시 "[사실]"과 "[감정]" 태그를 포함하세요.
            - [사실]: 사용자가 언급한 객관적 상황 (예: 시험 기간, 직장 갈등, 이별)
            - [감정]: 대화에서 드러난 감정 흐름 (예: 불안했으나 대화 후 안정)
            - 200자를 넘기지 마세요.
            - 예시: "[사실] 기말시험 준비 중이고 수면 부족 호소 [감정] 불안감 높았으나 대화 후 다소 안정"
            """;

    // ── 페르소나 프롬프트 ──
    private static final String FRIEND_PROMPT = """
            [페르소나: 친구형]
            당신은 사용자의 오랜 친한 친구입니다.
            - 반말을 사용합니다 (preferredTone과 무관하게 반말 고정). 자연스럽고 따뜻하게 대화합니다.
            - 사용자의 감정에 먼저 공감하고, 조언은 요청받을 때만 합니다.
            - 이모티콘을 가끔 사용해도 좋습니다 (ㅋㅋ, ㅠㅠ, ㅎㅎ 정도).
            - 가볍게 농담도 할 수 있지만, 사용자가 진지할 때는 진지하게 대응합니다.
            - 질문은 한 번에 하나만 합니다. 질문 폭탄은 금지입니다.
            - 사용자가 말하지 않은 것을 추측해서 단정하지 않습니다.

            말투 예시:
            - "야 그거 진짜 힘들었겠다. 그래서 어떻게 됐어?"
            - "그 상황에서 그렇게 느끼는 거 당연한 거야 ㅠ"
            - "뭔가 해결책 찾기보다 그냥 들어줬으면 해? 그러면 말해줘"
            """;

    private static final String COUNSELOR_PROMPT = """
            [페르소나: 상담사형]
            당신은 훈련받은 정서 지원 상담사처럼 대화합니다.
            - 정중한 존댓말을 사용합니다 (preferredTone과 무관하게 존댓말 고정).
            - 비지시적 상담 기법을 사용합니다: 반영(feeling reflection), 요약, 개방형 질문.
            - "왜"로 시작하는 질문 대신 "어떻게", "무엇이" 질문을 사용합니다.
            - 사용자의 감정을 먼저 반영한 후 질문합니다.
            - 조언이나 해결책을 먼저 제시하지 않습니다.
            - 구조화된 응답: 감정 반영 → 탐색 질문 순서를 지킵니다.

            말투 예시:
            - "그 상황에서 많이 지치고 외로우셨겠네요. 그 감정이 언제부터 시작됐는지 조금 더 이야기해 주실 수 있나요?"
            - "지금 말씀하신 부분이 특히 많이 힘드신 것 같아요. 좀 더 이야기해 주시겠어요?"
            """;

    public String build(PromptContext context) {
        StringBuilder sb = new StringBuilder();

        // 블록 ① 공통 안전 규칙
        sb.append(SAFETY_BLOCK).append("\n");

        // 블록 ② 사용자 프로필 요약
        sb.append(buildProfileBlock(context)).append("\n");

        // 블록 ③ 세션 정보 (페르소나 + recentSummary)
        sb.append(buildSessionBlock(context)).append("\n");

        // 블록 ④ 응답 형식 규칙
        sb.append(RESPONSE_FORMAT_BLOCK);

        return sb.toString();
    }

    private String buildProfileBlock(PromptContext context) {
        StringBuilder sb = new StringBuilder("[사용자 정보]\n");
        sb.append("- 닉네임: ").append(context.getNickname()).append("\n");
        sb.append("- 선호 말투: ").append(context.getPreferredTone()).append("\n");

        if (context.getPersonality() != null && !context.getPersonality().isBlank()) {
            sb.append("- 성격: ").append(context.getPersonality()).append("\n");
        }
        if (context.getHobbies() != null && !context.getHobbies().isEmpty()) {
            sb.append("- 주요 취미: ").append(joinList(context.getHobbies())).append("\n");
        }
        if (context.getConcerns() != null && !context.getConcerns().isEmpty()) {
            sb.append("- 자주 언급하는 고민: ").append(joinList(context.getConcerns())).append("\n");
        }
        return sb.toString();
    }

    private String buildSessionBlock(PromptContext context) {
        StringBuilder sb = new StringBuilder();

        // 페르소나 프롬프트
        sb.append(getPersonaPrompt(context.getPersonaType())).append("\n");

        // 세션 정보
        sb.append("[현재 세션]\n");
        sb.append("- 대화 모드: ").append(getPersonaDescription(context.getPersonaType())).append("\n");

        if (context.getRecentSummary() != null && !context.getRecentSummary().isBlank()) {
            sb.append("- 이전 대화 요약: ").append(context.getRecentSummary()).append("\n");
        }

        return sb.toString();
    }

    private String getPersonaPrompt(PersonaType type) {
        return switch (type) {
            case FRIEND -> FRIEND_PROMPT;
            case COUNSELOR -> COUNSELOR_PROMPT;
        };
    }

    private String getPersonaDescription(PersonaType type) {
        return switch (type) {
            case FRIEND -> "친구형 (편하고 친근하게 대화)";
            case COUNSELOR -> "상담사형 (전문적이고 체계적으로 대화)";
        };
    }

    private String joinList(List<String> list) {
        return list.stream().collect(Collectors.joining(", "));
    }
}
