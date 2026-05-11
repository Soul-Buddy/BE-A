package com.soulbuddy.domain.emotion.repository;

import com.soulbuddy.domain.emotion.entity.EmotionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmotionLogRepository extends JpaRepository<EmotionLog, Long> {

    // GET /api/dashboard/me - 감정 태그별 카운트 집계
    @Query("SELECT e.emotionTag, COUNT(e) FROM EmotionLog e WHERE e.userId = :userId GROUP BY e.emotionTag")
    List<Object[]> countEmotionTagByUserId(@Param("userId") Long userId);
}
