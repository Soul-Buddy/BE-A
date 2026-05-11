package com.soulbuddy.domain.summary.repository;

import com.soulbuddy.domain.summary.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long> {

    Optional<Summary> findBySessionId(String sessionId);

    List<Summary> findAllBySessionIdIn(List<String> sessionIds);
}
