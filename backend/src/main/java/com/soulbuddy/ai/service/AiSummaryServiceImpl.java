package com.soulbuddy.ai.service;

import com.soulbuddy.ai.client.SummaryLlmClient;
import com.soulbuddy.ai.dto.ChatMessageDto;
import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.ai.parser.AiResponseParser;
import com.soulbuddy.global.enums.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryServiceImpl implements AiSummaryService {

    private final SummaryLlmClient summaryLlmClient;
    private final AiResponseParser aiResponseParser;

    @Override
    public SummaryResult summarize(List<ChatMessageDto> messages) {
        String dialog = buildDialog(messages);
        long start = System.currentTimeMillis();
        String raw = summaryLlmClient.summarize(dialog);
        log.info("HCX-007 요약 응답 시간: {}ms", System.currentTimeMillis() - start);
        if (raw == null) {
            return SummaryResult.builder()
                    .summaryText("요약 생성에 실패했습니다.")
                    .emotionDistribution(new HashMap<>())
                    .build();
        }
        return aiResponseParser.parseSummary(raw);
    }

    private String buildDialog(List<ChatMessageDto> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessageDto m : messages) {
            String role = m.getSender() == Sender.USER ? "사용자"
                    : (m.getSender() == Sender.ASSISTANT ? "AI" : "시스템");
            sb.append(role).append(": ").append(m.getContent()).append('\n');
        }
        return sb.toString();
    }
}
