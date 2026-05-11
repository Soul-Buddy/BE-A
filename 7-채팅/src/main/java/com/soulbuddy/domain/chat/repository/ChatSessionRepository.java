package com.soulbuddy.domain.chat.repository;

import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.global.enums.SessionStatus;
import com.soulbuddy.global.enums.SummaryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    // GET /api/sessions - 전체 (status 필터 없음)
    Page<ChatSession> findByUserIdAndDeletedAtIsNullOrderByStartedAtDesc(Long userId, Pageable pageable);

    // GET /api/sessions?status=ENDED 등 status 필터
    Page<ChatSession> findByUserIdAndStatusAndDeletedAtIsNullOrderByStartedAtDesc(
            Long userId, SessionStatus status, Pageable pageable);

    // 단건 조회 + 소유권 확인
    Optional<ChatSession> findByIdAndUserId(String id, Long userId);

    // POST /api/sessions - recentSummary 조회용 (직전 종료 세션)
    Optional<ChatSession> findTopByUserIdAndStatusOrderByEndedAtDesc(Long userId, SessionStatus status);

    // GET /api/dashboard/me - 요약 카드 목록 (summaryStatus=CREATED인 종료 세션)
    Page<ChatSession> findByUserIdAndStatusAndSummaryStatusAndDeletedAtIsNullOrderByEndedAtDesc(
            Long userId, SessionStatus status, SummaryStatus summaryStatus, Pageable pageable);
}
