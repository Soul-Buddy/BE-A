package com.soulbuddy.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    AUTH_001("AUTH_001", "인증에 실패했습니다."),
    AUTH_002("AUTH_002", "토큰이 만료되었습니다."),
    AUTH_003("AUTH_003", "유효하지 않은 토큰입니다."),
    AUTH_004("AUTH_004", "접근 권한이 없습니다."),

    // User
    USER_001("USER_001", "사용자를 찾을 수 없습니다."),
    USER_002("USER_002", "이미 존재하는 이메일입니다."),

    // Profile
    PROFILE_001("PROFILE_001", "프로필을 찾을 수 없습니다."),

    // Session
    SESSION_001("SESSION_001", "세션을 찾을 수 없습니다."),

    // AI
    AI_001("AI_001", "AI 응답 생성에 실패했습니다."),
    AI_002("AI_002", "AI 서비스가 일시적으로 사용 불가합니다."),
    AI_003("AI_003", "위험 감지 — 안전 응답이 반환되었습니다."),

    // DB
    DB_001("DB_001", "데이터 저장에 실패했습니다."),

    // Validation
    VALID_001("VALID_001", "요청 파라미터 유효성 오류입니다."),

    // Common
    COMMON_001("COMMON_001", "잘못된 요청입니다."),
    COMMON_002("COMMON_002", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;
}
