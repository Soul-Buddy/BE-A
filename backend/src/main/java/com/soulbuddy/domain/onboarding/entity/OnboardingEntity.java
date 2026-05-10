package com.soulbuddy.domain.onboarding.entity;

import com.soulbuddy.global.enums.Gender;
import com.soulbuddy.global.enums.PreferredTone; // 추가 필요 (FRIEND, COUNSELOR)
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding") // 사진의 테이블명 반영
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OnboardingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId; // FK -> users (UNIQUE)

    @Column(nullable = false, length = 50)
    private String nickname; // 온보딩 입력 닉네임

    private Integer age;

    @Enumerated(EnumType.STRING)
    private Gender gender; // MALE, FEMALE

    @Column(length = 100)
    private String occupation; // 직업 (자유 텍스트)

    @Column(name = "usage_intent", columnDefinition = "TEXT")
    private String usageIntent; // 앱 사용 의도

    @Column(columnDefinition = "TEXT")
    private String hobbies; // JSON 배열 문자열

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_tone", nullable = false)
    private PreferredTone preferredTone; // FRIEND, COUNSELOR

    @Column(name = "liked_things", columnDefinition = "TEXT")
    private String likedThings; // 좋아하는 것들 (JSON)

    @Column(name = "disliked_things", columnDefinition = "TEXT")
    private String dislikedThings; // 싫어하는 것들 (JSON)

    @Column(name = "personal_instruction", columnDefinition = "TEXT")
    private String personalInstruction; // 사전 가공된 프롬프트 캐싱

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt; // 완료 시점 (NULL이면 라우팅 불가)

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}