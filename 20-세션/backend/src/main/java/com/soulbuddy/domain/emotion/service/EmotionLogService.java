package com.soulbuddy.domain.emotion.service;

import com.soulbuddy.domain.emotion.entity.EmotionLog;
import com.soulbuddy.domain.emotion.repository.EmotionLogRepository;
import com.soulbuddy.global.enums.EmotionSource;
import com.soulbuddy.global.enums.EmotionTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmotionLogService {

    private final EmotionLogRepository emotionLogRepository;

    public void log(Long userId, String sessionId, Long messageId,
                    EmotionTag emotionTag, EmotionSource source) {
        emotionLogRepository.save(
                EmotionLog.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .messageId(messageId)
                        .emotionTag(emotionTag)
                        .source(source)
                        .build()
        );
    }
}
