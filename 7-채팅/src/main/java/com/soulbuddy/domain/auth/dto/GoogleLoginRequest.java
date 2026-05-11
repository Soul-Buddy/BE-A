package com.soulbuddy.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GoogleLoginRequest {

    @NotBlank
    private String googleIdToken;
}
