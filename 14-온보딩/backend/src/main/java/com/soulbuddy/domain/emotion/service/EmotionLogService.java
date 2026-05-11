package com.soulbuddy.domain.emotion.service;

import com.soulbuddy.domain.chat.entity.ChatSession;
import com.soulbuddy.domain.emotion.entity.EmotionLog;
import com.soulbuddy.domain.emotion.repository.EmotionLogRepository;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.repository.UserRepository;
import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionLogService {

    private final EmotionLogRepository emotionLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void save(Long userId, ChatSession session, EmotionTag emotionTag) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        EmotionLog log = EmotionLog.builder()
                .user(user)
                .session(session)
                .emotionTag(emotionTag)
                .build();

        emotionLogRepository.save(log);
    }

    public List<EmotionLog> getByUserId(Long userId) {
        return emotionLogRepository.findByUserId(userId);
    }
}
