package com.soulbuddy.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    AUTH_001("AUTH_001", "인증 토큰이 없거나 만료되었습니다."),
    AUTH_002("AUTH_002", "권한이 없습니다."),
    AUTH_003("AUTH_003", "유효하지 않은 토큰입니다."),
    AUTH_004("AUTH_004", "접근 권한이 없습니다."),

    USER_001("USER_001", "사용자를 찾을 수 없습니다."),
    USER_002("USER_002", "이미 존재하는 이메일입니다."),

    PROFILE_001("PROFILE_001", "프로필 데이터가 누락되었습니다."),

    SESSION_001("SESSION_001", "세션을 찾을 수 없습니다."),
    SESSION_002("SESSION_002", "이미 종료된 세션입니다."),

    AI_001("AI_001", "LLM(CLOVA Studio) API 호출에 실패했습니다."),
    AI_002("AI_002", "AI 응답 파싱에 실패했습니다 (fallback 반환)."),
    AI_003("AI_003", "위험이 감지되어 안전 응답을 반환합니다."),

    RAG_001("RAG_001", "Vector Store 검색에 실패했습니다 (RAG 건너뜀)."),

    SAFETY_001("SAFETY_001", "자가설문 응답이 누락되었습니다."),
    SAFETY_002("SAFETY_002", "상담센터 데이터를 조회할 수 없습니다."),

    DB_001("DB_001", "데이터 저장에 실패했습니다."),

    VALID_001("VALID_001", "요청 파라미터 유효성 오류입니다."),

    COMMON_001("COMMON_001", "잘못된 요청입니다."),
    COMMON_002("COMMON_002", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;
}
