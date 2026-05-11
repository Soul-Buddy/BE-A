package com.soulbuddy.domain.emotion.repository;

import com.soulbuddy.domain.emotion.entity.EmotionLog;
import com.soulbuddy.global.enums.EmotionTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmotionLogRepository extends JpaRepository<EmotionLog, Long> {

    List<EmotionLog> findByUserId(Long userId);

    List<EmotionLog> findBySessionId(String sessionId);
}
