package com.soulbuddy.ai.prompt;

import com.soulbuddy.global.enums.InterventionType;
import com.soulbuddy.global.enums.PersonaType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * system_prompts_final.txt 의 섹션별 프롬프트를 메모리로 로드.
 * - common: 공통 규칙 (페르소나 AI에 항상 주입)
 * - persona FRIEND/COUNSELOR
 * - intervention 11종 (루미용 하위 지침)
 * - classifier emotion/risk/intervention
 * - opening greeting (visitState 인사말 지침)
 */
@Slf4j
@Component
public class SystemPromptLoader {

    private final ResourceLoader resourceLoader;

    @Value("${soulbuddy.prompt.system-prompts-path}")
    private String promptsPath;

    private String commonRules;
    private final Map<PersonaType, String> personaPrompts = new EnumMap<>(PersonaType.class);
    private final Map<InterventionType, String> interventionPrompts = new EnumMap<>(InterventionType.class);
    private String classifierEmotion;
    private String classifierRisk;
    private String classifierIntervention;
    private String openingGreeting;

    public SystemPromptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void load() {
        try {
            Resource resource = resourceLoader.getResource(promptsPath);
            if (!resource.exists()) {
                log.warn("system_prompts_final.txt 파일을 찾을 수 없습니다: {}", promptsPath);
                applyDefaults();
                return;
            }
            String content;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                content = sb.toString();
            }
            parse(content);
            log.info("system_prompts_final.txt 로드 완료. persona={}, intervention={}",
                    personaPrompts.size(), interventionPrompts.size());
        } catch (Exception e) {
            log.error("system_prompts_final.txt 로드 실패: {}", e.getMessage(), e);
            applyDefaults();
        }
    }

    private void parse(String content) {
        commonRules = extractBetween(content,
                "[ 공통 규칙",
                "================================================================\n1. 친구형");
        personaPrompts.put(PersonaType.FRIEND,
                extractBetween(content,
                        "1. 친구형 페르소나 AI",
                        "================================================================\n2. 상담사형"));
        personaPrompts.put(PersonaType.COUNSELOR,
                extractBetween(content,
                        "2. 상담사형 페르소나 AI",
                        "[ 2-A. 루미"));

        interventionPrompts.put(InterventionType.SYMPATHY_SUPPORT,
                extractBetween(content, "▶ sympathy_support", "▶ clarification_reflection"));
        interventionPrompts.put(InterventionType.CLARIFICATION_REFLECTION,
                extractBetween(content, "▶ clarification_reflection", "▶ structuring"));
        interventionPrompts.put(InterventionType.STRUCTURING,
                extractBetween(content, "▶ structuring", "▶ goal_setting"));
        interventionPrompts.put(InterventionType.GOAL_SETTING,
                extractBetween(content, "▶ goal_setting", "▶ cognitive_restructuring"));
        interventionPrompts.put(InterventionType.COGNITIVE_RESTRUCTURING,
                extractBetween(content, "▶ cognitive_restructuring", "▶ behavioral_intervention"));
        interventionPrompts.put(InterventionType.BEHAVIORAL_INTERVENTION,
                extractBetween(content, "▶ behavioral_intervention", "▶ emotional_regulation_education_training"));
        interventionPrompts.put(InterventionType.EMOTIONAL_REGULATION_EDUCATION_TRAINING,
                extractBetween(content, "▶ emotional_regulation_education_training", "▶ information_provision"));
        interventionPrompts.put(InterventionType.INFORMATION_PROVISION,
                extractBetween(content, "▶ information_provision", "▶ process_feedback"));
        interventionPrompts.put(InterventionType.PROCESS_FEEDBACK,
                extractBetween(content, "▶ process_feedback", "▶ task_assignment"));
        interventionPrompts.put(InterventionType.TASK_ASSIGNMENT,
                extractBetween(content, "▶ task_assignment", "▶ training_of_coping_skills"));
        interventionPrompts.put(InterventionType.TRAINING_OF_COPING_SKILLS,
                extractBetween(content, "▶ training_of_coping_skills", "[루미 응답 형식 규칙]"));

        classifierRisk = extractBetween(content,
                "3. 위험도 분류 AI",
                "================================================================\n4. 상담 발화");
        classifierIntervention = extractBetween(content,
                "4. 상담 발화 개입 유형 분류 AI",
                "================================================================\n5. 감정 분류");
        classifierEmotion = extractBetween(content,
                "5. 감정 분류 AI",
                "================================================================\n6. 인사말");
        openingGreeting = extractBetween(content,
                "6. 인사말 지침",
                "================================================================\n[ 끝 ]");
    }

    private String extractBetween(String src, String startMarker, String endMarker) {
        int s = src.indexOf(startMarker);
        if (s < 0) return "";
        int e = src.indexOf(endMarker, s + startMarker.length());
        if (e < 0) e = src.length();
        return src.substring(s, e).trim();
    }

    private void applyDefaults() {
        commonRules = "당신은 정서 지원 AI입니다. 진단·처방·병명 단정 금지. 위험 표현 시 안전 응답 우선.";
        personaPrompts.put(PersonaType.FRIEND, "친구처럼 따뜻한 반말로 짧게 공감하세요.");
        personaPrompts.put(PersonaType.COUNSELOR, "정중한 존댓말로 비지시적 상담 기법을 사용하세요.");
        for (InterventionType it : InterventionType.values()) {
            interventionPrompts.put(it, "");
        }
        classifierEmotion = "다음 6종 중 하나만 출력: HAPPY/SAD/ANGRY/ANXIOUS/HURT/EMBARRASSED 또는 한국어 기쁨/슬픔/분노/불안/상처/당황";
        classifierRisk = "출력 형식: {\"risk\":\"LOW|MEDIUM|HIGH\"}";
        classifierIntervention = "다음 11종 영문 코드 중 하나만 출력";
        openingGreeting = "";
    }

    public String getCommonRules() {
        return commonRules == null ? "" : commonRules;
    }

    public String getPersonaPrompt(PersonaType type) {
        return personaPrompts.getOrDefault(type, "");
    }

    public String getInterventionPrompt(InterventionType type) {
        if (type == null) return "";
        return interventionPrompts.getOrDefault(type, "");
    }

    public String getClassifierEmotion() {
        return classifierEmotion == null ? "" : classifierEmotion;
    }

    public String getClassifierRisk() {
        return classifierRisk == null ? "" : classifierRisk;
    }

    public String getClassifierIntervention() {
        return classifierIntervention == null ? "" : classifierIntervention;
    }

    public String getOpeningGreeting() {
        return openingGreeting == null ? "" : openingGreeting;
    }

    public Map<String, Object> debugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("commonRulesLen", getCommonRules().length());
        info.put("personaFriendLen", getPersonaPrompt(PersonaType.FRIEND).length());
        info.put("personaCounselorLen", getPersonaPrompt(PersonaType.COUNSELOR).length());
        info.put("interventionLoaded", interventionPrompts.size());
        return info;
    }
}
