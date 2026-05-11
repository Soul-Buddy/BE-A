package com.soulbuddy.domain.summary.repository;

import com.soulbuddy.domain.summary.entity.Summary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long> {

    Optional<Summary> findBySessionId(String sessionId);

    Page<Summary> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Summary> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
