package com.soulbuddy.domain.chat.controller;

import com.soulbuddy.domain.chat.dto.response.PersonaResponse;
import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonaResponse>>> getPersonas() {
        List<PersonaResponse> personas = Arrays.stream(PersonaType.values())
                .map(PersonaResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(personas));
    }
}
