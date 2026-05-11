package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.Sender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 레이어 전용 대화 메시지 DTO.
 * 헌영의 ChatMessage entity에 직접 의존하지 않기 위해 분리.
 * production에서는 헌영 ChatService가 entity → DTO 변환해서 전달.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private Sender sender;
    private String content;
    private LocalDateTime createdAt;
}
