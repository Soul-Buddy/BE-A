package com.soulbuddy.domain.onboarding.repository;

import com.soulbuddy.domain.onboarding.entity.OnboardingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OnboardingRepository extends JpaRepository<OnboardingEntity, Long> {


    Optional<OnboardingEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}