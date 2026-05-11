package com.soulbuddy.domain.chat.controller;

import com.soulbuddy.domain.chat.dto.response.PersonaResponse;
import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Persona", description = "페르소나 목록")
@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    @Operation(summary = "페르소나 목록 조회 (포코/루미)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonaResponse>>> getPersonas() {
        List<PersonaResponse> personas = Arrays.stream(PersonaType.values())
                .map(PersonaResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(personas));
    }
}
