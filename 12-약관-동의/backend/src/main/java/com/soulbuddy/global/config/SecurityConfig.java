package com.soulbuddy.global.config;

import com.soulbuddy.global.auth.JwtAuthenticationFilter;
import com.soulbuddy.global.auth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // 인증 없이 접근 가능한 URL 목록
    private static final String[] PUBLIC_URLS = {
            "/api/health",
            "/api/auth/**",
            "/api/personas",
            // Swagger 관련 경로 (v3로 수정 및 리소스 경로 추가)
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            // 약관 동의 API 테스트를 위해 임시 허용 (작성하신 실제 경로에 맞춰주세요)
            "/api/agreement/**",
            "/oauth2/**",
            "/login/oauth2/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화 (Stateless 방식이므로 보통 끕니다)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 세션을 사용하지 않음 (JWT 방식)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 경로별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll() // 위 목록은 모두 허용
                        .anyRequest().authenticated()             // 나머지는 인증 필요
                )

                // 4. 인증 실패 시 401 에러 리턴
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                // 5. OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                )

                // 6. JWT 필터를 시큐리티 필터 체인에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}