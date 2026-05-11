package com.soulbuddy.domain.user.service;

public interface ProfileQueryService {
    String getNicknameByUserId(Long userId);
    String getPersonalInstructionByUserId(Long userId);
}
