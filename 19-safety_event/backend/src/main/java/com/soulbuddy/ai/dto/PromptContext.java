package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.InterventionType;
import com.soulbuddy.global.enums.PersonaType;
import com.soulbuddy.global.enums.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PromptContext {

    private String personalInstruction;
    private String nickname;
    private PersonaType personaType;

    private String recentSummary;
    private List<TurnMessage> recentTurns;

    private EmotionTag classifiedEmotion;
    private RiskLevel classifiedRisk;
    private InterventionType classifiedIntervention;

    private List<RagChunkRef> ragTop3;

    private boolean firstTurn;
    private OpeningMeta openingMeta;

    @Getter
    @Builder
    public static class TurnMessage {
        private String role;
        private String content;
    }

    @Getter
    @Builder
    public static class RagChunkRef {
        private String date;
        private String situation;
        private String emotion;
        private String thought;
    }

    @Getter
    @Builder
    public static class OpeningMeta {
        private String visitState;
        private String priorTopic;
        private String priorSeverity;
        private boolean priorUnresolved;
    }
}
