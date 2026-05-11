package com.soulbuddy.domain.chat.entity;

import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "persona_type", nullable = false)
    private PersonaType personaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(length = 255)
    private String title;

    @Builder.Default
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
