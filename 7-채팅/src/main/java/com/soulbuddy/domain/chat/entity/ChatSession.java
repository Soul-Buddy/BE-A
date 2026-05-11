package com.soulbuddy.domain.chat.entity;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.enums.SessionStatus;
import com.soulbuddy.global.enums.SummaryStatus;
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
    @Column(name = "id", length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "persona_type", nullable = false, length = 20)
    private PersonaType personaType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "pre_chat_emotion", length = 20)
    private EmotionTag preChatEmotion;

    @Builder.Default
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", nullable = false, length = 20)
    private SummaryStatus summaryStatus = SummaryStatus.NOT_CREATED;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updatePreChatEmotion(EmotionTag emotion) {
        this.preChatEmotion = emotion;
    }

    public void end(SummaryStatus summaryStatus) {
        this.status = SessionStatus.ENDED;
        this.endedAt = LocalDateTime.now();
        this.summaryStatus = summaryStatus;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == SessionStatus.ACTIVE;
    }
}
