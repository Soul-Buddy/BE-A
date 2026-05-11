package com.soulbuddy.ai.service;

import com.soulbuddy.ai.dto.ChatMessageDto;
import com.soulbuddy.ai.dto.SummaryResult;

import java.util.List;

/**
 * 세션 종료 요약 생성 (HCX-007).
 * DB는 직접 호출하지 않습니다.
 *
 * production: 헌영 SessionService가 ChatMessage entity → ChatMessageDto 변환해 호출.
 */
public interface AiSummaryService {

    SummaryResult summarize(List<ChatMessageDto> messages);
}
