package com.soulbuddy.ai.service;

import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.domain.chat.entity.ChatMessage;

import java.util.List;

/**
 * 세션 종료 요약 생성 서비스
 * DB를 직접 호출하지 않습니다.
 */
public interface AiSummaryService {

    SummaryResult summarize(List<ChatMessage> messages);
}
