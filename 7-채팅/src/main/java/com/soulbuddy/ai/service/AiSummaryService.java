package com.soulbuddy.ai.service;

import com.soulbuddy.ai.dto.ChatMessageDto;
import com.soulbuddy.ai.dto.SummaryResult;

import java.util.List;

public interface AiSummaryService {

    SummaryResult summarize(String sessionId, Long userId, List<ChatMessageDto> messages);
}
