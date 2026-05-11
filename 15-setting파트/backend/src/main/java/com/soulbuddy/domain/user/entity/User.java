package com.soulbuddy.domain.user.entity;

import com.soulbuddy.global.enums.Gender;
import com.soulbuddy.global.enums.Provider;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    // 1. 소셜 로그인 공급자 (GOOGLE, KAKAO 등)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    // 2. 소셜 로그인 고유 ID (식별값) 추가 -> 이 부분이 없어서 에러가 난 것입니다.
    private String providerId;

    private String job;

    private String ageGroup;

    private String profileImageUrl;

    private boolean dailyCheckInAlarm;

    private boolean cheerMessageAlarm;

    private String email;

    private String password;

    private boolean termsAgreed;

    private LocalDateTime termsAgreedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    // --- 비즈니스 로직 메서드 ---

    public boolean hasAgreedTerms() {
        return this.termsAgreed;
    }

    public void agreeTerms() {
        this.termsAgreed = true;
        this.termsAgreedAt = LocalDateTime.now();
    }
}