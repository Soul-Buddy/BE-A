package com.soulbuddy.domain.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * RAG 인덱스 entity.
 * FK 관계는 ID 컬럼만 보유 (다른 도메인 entity 의존 제거).
 * DB 제약(FOREIGN KEY)은 schema_0502_v4.sql에서 그대로 유지됨.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "rag_chunks")
public class RagChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "summary_id")
    private Long summaryId;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "pulling_text", columnDefinition = "TEXT")
    private String pullingText;

    @Column(name = "vector_ref", length = 255)
    private String vectorRef;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void markIndexed(String vectorRef) {
        this.vectorRef = vectorRef;
        this.indexedAt = LocalDateTime.now();
    }

    public void updatePullingText(String pullingText) {
        this.pullingText = pullingText;
    }
}
