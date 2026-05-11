package com.soulbuddy.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false, length = 255)
    private String providerId;

    @Column(length = 20)
    private String role = "USER";       // USER | ADMIN

    // [02 약관동의] "동의하고 시작하기" 탭 시점
    @Column(name = "terms_agreed_at")
    private LocalDateTime termsAgreedAt;

    // 개인정보 수집·이용 동의 시점 (약관과 분리)
    @Column(name = "privacy_agreed_at")
    private LocalDateTime privacyAgreedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ── 소셜 로그인 최초 가입용 정적 팩토리 ──────────────────────────────
    public static User ofSocial(String email, String nickname,
                                Provider provider, String providerId) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.provider = provider;
        user.providerId = providerId;
        user.role = "USER";
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        return user;
    }

    // ── [02. 약관동의] 동의 처리 메서드 ──────────────────────────────────

    /** 서비스 이용약관 동의 */
    public void agreeTerms(LocalDateTime agreedAt) {
        this.termsAgreedAt = agreedAt;
        this.updatedAt = LocalDateTime.now();
    }

    /** 개인정보 수집·이용 동의 (약관과 별도 분리 기록) */
    public void agreePrivacy(LocalDateTime agreedAt) {
        this.privacyAgreedAt = agreedAt;
        this.updatedAt = LocalDateTime.now();
    }

    /** 두 약관 모두 동의 완료 여부 확인 */
    public boolean hasAgreedAll() {
        return termsAgreedAt != null && privacyAgreedAt != null;
    }

    /** 마지막 로그인 갱신 */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Provider {
        GOOGLE, KAKAO
    }
}