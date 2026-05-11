package com.soulbuddy.domain.user.entity;

import com.soulbuddy.global.converter.StringListConverter;
import com.soulbuddy.global.enums.Gender;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 100)
    private String occupation;

    @Column(name = "usage_intent", columnDefinition = "TEXT")
    private String usageIntent;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> hobbies;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_tone", nullable = false, length = 20)
    private PreferredTone preferredTone;

    @Convert(converter = StringListConverter.class)
    @Column(name = "liked_things", columnDefinition = "TEXT")
    private List<String> likedThings;

    @Convert(converter = StringListConverter.class)
    @Column(name = "disliked_things", columnDefinition = "TEXT")
    private List<String> dislikedThings;

    @Column(name = "personal_instruction", columnDefinition = "TEXT")
    private String personalInstruction;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(String nickname, Integer age, Gender gender, String occupation,
                       String usageIntent, List<String> hobbies, PreferredTone preferredTone,
                       List<String> likedThings, List<String> dislikedThings) {
        this.nickname = nickname;
        this.age = age;
        this.gender = gender;
        this.occupation = occupation;
        this.usageIntent = usageIntent;
        this.hobbies = hobbies;
        this.preferredTone = preferredTone;
        this.likedThings = likedThings;
        this.dislikedThings = dislikedThings;
        rebuildPersonalInstruction();
    }

    public void completeOnboarding() {
        this.onboardingCompletedAt = LocalDateTime.now();
        rebuildPersonalInstruction();
    }

    private void rebuildPersonalInstruction() {
        StringBuilder sb = new StringBuilder();
        sb.append("닉네임: ").append(nickname).append("\n");
        if (age != null) sb.append("나이: ").append(age).append("세\n");
        if (gender != null) sb.append("성별: ").append(gender.name()).append("\n");
        if (occupation != null && !occupation.isBlank()) sb.append("직업: ").append(occupation).append("\n");
        if (usageIntent != null && !usageIntent.isBlank()) sb.append("사용 목적: ").append(usageIntent).append("\n");
        if (hobbies != null && !hobbies.isEmpty()) sb.append("취미: ").append(String.join(", ", hobbies)).append("\n");
        if (likedThings != null && !likedThings.isEmpty()) sb.append("좋아하는 것: ").append(String.join(", ", likedThings)).append("\n");
        if (dislikedThings != null && !dislikedThings.isEmpty()) sb.append("싫어하는 것: ").append(String.join(", ", dislikedThings)).append("\n");
        this.personalInstruction = sb.toString().trim();
    }
}
