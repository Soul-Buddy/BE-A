package com.soulbuddy.domain.user.service;

import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.repository.UserRepository;
import com.soulbuddy.global.enums.Provider;
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByProviderAndProviderId(Provider provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId);
    }

    @Transactional
    public User createUser(String email, String nickname, Provider provider, String providerId) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .provider(provider)
                .providerId(providerId)
                .build();
        return userRepository.save(user);
    }
}
