package com.soulbuddy.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulbuddy.domain.auth.dto.LoginResponse;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.repository.ProfileRepository;
import com.soulbuddy.domain.user.repository.UserRepository;
import com.soulbuddy.global.auth.JwtTokenProvider;
import com.soulbuddy.global.enums.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    private final RestClient googleRestClient = RestClient.create();

    @Transactional
    public LoginResponse loginWithGoogle(String googleIdToken) {
        JsonNode tokenInfo = verifyGoogleToken(googleIdToken);

        String providerId = tokenInfo.get("sub").asText();
        String email      = tokenInfo.get("email").asText();
        String name       = tokenInfo.has("name") ? tokenInfo.get("name").asText() : "사용자";

        boolean[] isNewUser = {false};
        User user = userRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId)
                .orElseGet(() -> {
                    isNewUser[0] = true;
                    return userRepository.save(User.ofSocial(email, name, Provider.GOOGLE, providerId));
                });

        user.updateLastLogin();

        boolean onboardingCompleted = profileRepository.findByUserId(user.getId())
                .map(p -> p.getOnboardingCompletedAt() != null)
                .orElse(false);

        return LoginResponse.builder()
                .accessToken(jwtTokenProvider.createAccessToken(user.getId()))
                .refreshToken(jwtTokenProvider.createRefreshToken(user.getId()))
                .userId(user.getId())
                .isNewUser(isNewUser[0])
                .termsAgreed(user.hasAgreedAll())
                .onboardingCompleted(onboardingCompleted)
                .build();
    }

    private JsonNode verifyGoogleToken(String idToken) {
        try {
            String response = googleRestClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Google token 검증 실패: {}", e.getMessage());
            throw new RuntimeException("유효하지 않은 Google 토큰입니다.");
        }
    }
}
