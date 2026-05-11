package com.soulbuddy.ai.client;

import com.soulbuddy.ai.dto.ChatResponse;

import java.util.List;
import java.util.Map;

/**
 * LLM API 호출 인터페이스 (OpenAI GPT-4o 전용)
 * 타임아웃: 30초 / 재시도: 최대 2회
 * 실패 시 AiResponseParser.fallback() 호출
 */
public interface LlmClient {

    /**
     * OpenAI Chat Completions API 호출
     *
     * @param messages [{role, content}, ...] 형태의 메시지 목록
     * @return GPT-4o 원본 응답 텍스트 (JSON 파싱 전)
     */
    String call(List<Map<String, String>> messages);
}
