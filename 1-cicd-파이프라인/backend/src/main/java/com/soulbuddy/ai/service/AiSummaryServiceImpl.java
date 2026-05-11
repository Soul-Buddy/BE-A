package com.soulbuddy.ai.service;

import com.soulbuddy.ai.client.LlmClient;
import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.ai.parser.AiResponseParser;
import com.soulbuddy.domain.chat.entity.ChatMessage;
import com.soulbuddy.global.enums.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryServiceImpl implements AiSummaryService {

    private final LlmClient llmClient;
    private final AiResponseParser aiResponseParser;

    private static final String SUMMARY_PROMPT_TEMPLATE = """
            아래는 사용자와 나눈 전체 대화입니다.
            다음 형식의 JSON으로만 응답하세요.

            {
              "summaryText": "대화의 핵심 내용을 2~3문장으로 요약",
              "dominantEmotion": "ANXIOUS | SAD | CALM | HAPPY | NEUTRAL | ANGRY 중 하나",
              "memoryHint": "[사실] 객관적 상황 정보 [감정] 감정 흐름 요약 (없으면 null, 최대 200자)"
            }

            memoryHint 작성 규칙:
            - 반드시 "[사실]"과 "[감정]" 태그를 포함하세요.
            - [사실]: 사용자가 언급한 객관적 상황
            - [감정]: 전체 대화의 감정 흐름
            - 200자를 넘기지 마세요.

            [전체 대화]
            """;

    @Override
    public SummaryResult summarize(List<ChatMessage> messages) {
        String conversationText = buildConversationText(messages);
        String prompt = SUMMARY_PROMPT_TEMPLATE + conversationText;

        List<Map<String, String>> llmMessages = List.of(
                Map.of("role", "system", "content", prompt)
        );

        long startTime = System.currentTimeMillis();
        String rawJson;
        try {
            rawJson = llmClient.call(llmMessages);
        } catch (Exception e) {
            log.error("요약 LLM 호출 실패: {}", e.getMessage());
            return SummaryResult.builder()
                    .summaryText("요약 생성에 실패했습니다.")
                    .dominantEmotion("NEUTRAL")
                    .memoryHint(null)
                    .build();
        }
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("요약 LLM 응답 시간: {}ms", elapsed);

        return aiResponseParser.parseSummary(rawJson);
    }

    private String buildConversationText(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            String sender = msg.getSender() == Sender.USER ? "사용자" : "AI";
            sb.append(sender).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }
}
