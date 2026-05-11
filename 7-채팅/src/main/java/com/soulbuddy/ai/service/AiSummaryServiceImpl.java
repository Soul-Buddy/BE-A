package com.soulbuddy.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulbuddy.ai.client.SummaryLlmClient;
import com.soulbuddy.ai.dto.ChatMessageDto;
import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.ai.parser.AiResponseParser;
import com.soulbuddy.domain.summary.entity.Summary;
import com.soulbuddy.domain.summary.repository.SummaryRepository;
import com.soulbuddy.global.enums.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryServiceImpl implements AiSummaryService {

    private final SummaryLlmClient summaryLlmClient;
    private final AiResponseParser aiResponseParser;
    private final SummaryRepository summaryRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SummaryResult summarize(String sessionId, Long userId, List<ChatMessageDto> messages) {
        String dialog = buildDialog(messages);
        long start = System.currentTimeMillis();
        String raw = summaryLlmClient.summarize(dialog);
        log.info("HCX-007 요약 응답 시간: {}ms", System.currentTimeMillis() - start);

        SummaryResult result;
        if (raw == null) {
            result = SummaryResult.builder()
                    .summaryText("요약 생성에 실패했습니다.")
                    .emotionDistribution(new HashMap<>())
                    .build();
        } else {
            result = aiResponseParser.parseSummary(raw);
        }

        saveSummary(sessionId, userId, result);
        return result;
    }

    private void saveSummary(String sessionId, Long userId, SummaryResult result) {
        String distributionJson = null;
        if (result.getEmotionDistribution() != null) {
            try {
                distributionJson = objectMapper.writeValueAsString(result.getEmotionDistribution());
            } catch (Exception e) {
                log.warn("emotionDistribution 직렬화 실패: {}", e.getMessage());
            }
        }
        summaryRepository.save(Summary.builder()
                .sessionId(sessionId)
                .userId(userId)
                .summaryText(result.getSummaryText())
                .situationText(result.getSituationText())
                .emotionText(result.getEmotionText())
                .thoughtText(result.getThoughtText())
                .dominantEmotion(result.getDominantEmotion())
                .emotionDistribution(distributionJson)
                .emotionChange(result.getEmotionChange())
                .quoteText(result.getQuoteText())
                .memoryHint(result.getMemoryHint())
                .build());
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
