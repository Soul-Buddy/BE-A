package com.soulbuddy.domain.user.controller;

import com.soulbuddy.domain.user.dto.PersonaResponse;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Persona", description = "페르소나 목록")
@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private static final List<PersonaResponse> PERSONAS = List.of(
            PersonaResponse.builder()
                    .personaType("FRIEND")
                    .displayName("친구형")
                    .description("편하고 친근하게 대화해요")
                    .toneSample("야 그거 진짜 힘들었겠다~")
                    .build(),
            PersonaResponse.builder()
                    .personaType("COUNSELOR")
                    .displayName("상담사형")
                    .description("전문적이고 체계적으로 대화해요")
                    .toneSample("그 상황에서 어떤 감정을 느끼셨나요?")
                    .build()
    );

    @Operation(summary = "페르소나 목록 조회", description = "사용 가능한 페르소나 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonaResponse>>> getPersonas() {
        return ResponseEntity.ok(ApiResponse.success(PERSONAS));
    }
}
