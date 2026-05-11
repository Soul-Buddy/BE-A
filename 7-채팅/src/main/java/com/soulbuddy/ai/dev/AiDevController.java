package com.soulbuddy.ai.dev;

import com.soulbuddy.ai.dto.ChatMessageDto;
import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.ai.dto.OpeningContext;
import com.soulbuddy.ai.dto.PromptContext;
import com.soulbuddy.ai.dto.SummaryResult;
import com.soulbuddy.ai.service.AiChatService;
import com.soulbuddy.ai.service.AiSummaryService;
import com.soulbuddy.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 지훈 단독 dev 컨트롤러. AI 파이프라인을 외부 도메인 의존 없이 단독 검증.
 * production에서는 헌영 ChatController/SessionController가 진입점이며 본 컨트롤러는 제거 예정.
 *
 * 정본 swagger: TestFolder/swagger/SoulBuddy_API_jihun_AI.json
 */
@Slf4j
@RestController
@RequestMapping("/api/dev/ai-test")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiDevController {

    private final AiChatService aiChatService;
    private final AiSummaryService aiSummaryService;

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody DevChatRequest body) {
        long recentHigh = body.getRecentHighCount() != null ? body.getRecentHighCount() : 0L;
        ChatResponse response = aiChatService.process(body.getRequest(), body.getContext(), recentHigh);
        return ApiResponse.success(response);
    }

    @PostMapping("/opening")
    public ApiResponse<Map<String, String>> opening(@Valid @RequestBody OpeningContext body) {
        String message = aiChatService.openingMessage(body);
        return ApiResponse.success(Map.of(
                "openingMessage", message,
                "aiModel", body.getPersonaType().aiModelTag()
        ));
    }

    @PostMapping("/summarize")
    public ApiResponse<SummaryResult> summarize(@Valid @RequestBody SummarizeRequest body) {
        String sessionId = body.getSessionId() != null ? body.getSessionId() : "dev-test-session";
        Long userId = body.getUserId() != null ? body.getUserId() : 0L;
        SummaryResult result = aiSummaryService.summarize(sessionId, userId, body.getMessages());
        return ApiResponse.success(result);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DevChatRequest {
        @Valid
        private ChatRequest request;
        @Valid
        private PromptContext context;
        private Long recentHighCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SummarizeRequest {
        private String sessionId;
        private Long userId;
        private List<ChatMessageDto> messages;
    }
}
