package com.soulbuddy.domain.user.repository;

import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.global.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
