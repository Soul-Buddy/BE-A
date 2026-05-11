package com.soulbuddy.domain.rag.repository;

import com.soulbuddy.domain.rag.entity.RagChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RagChunkRepository extends JpaRepository<RagChunk, Long> {

    List<RagChunk> findByUserId(Long userId);

    List<RagChunk> findBySessionId(String sessionId);
}
