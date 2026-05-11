package com.soulbuddy.domain.chat.repository;

import com.soulbuddy.domain.chat.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    Page<ChatSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
