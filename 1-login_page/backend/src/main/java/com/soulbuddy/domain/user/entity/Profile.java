package com.soulbuddy.domain.user.entity;

import com.soulbuddy.global.converter.StringListConverter;
import com.soulbuddy.global.enums.PreferredTone;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 50)
    private String nickname;

    private Integer age;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> hobbies;

    @Column(columnDefinition = "TEXT")
    private String personality;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> concerns;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_tone", nullable = false, length = 50)
    private PreferredTone preferredTone;

    @Convert(converter = StringListConverter.class)
    @Column(name = "liked_things", columnDefinition = "TEXT")
    private List<String> likedThings;

    @Convert(converter = StringListConverter.class)
    @Column(name = "disliked_things", columnDefinition = "TEXT")
    private List<String> dislikedThings;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
