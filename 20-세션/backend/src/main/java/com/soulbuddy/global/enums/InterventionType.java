package com.soulbuddy.global.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum InterventionType {
    SYMPATHY_SUPPORT("sympathy_support"),
    CLARIFICATION_REFLECTION("clarification_reflection"),
    STRUCTURING("structuring"),
    GOAL_SETTING("goal_setting"),
    COGNITIVE_RESTRUCTURING("cognitive_restructuring"),
    BEHAVIORAL_INTERVENTION("behavioral_intervention"),
    EMOTIONAL_REGULATION_EDUCATION_TRAINING("emotional_regulation_education_training"),
    INFORMATION_PROVISION("information_provision"),
    PROCESS_FEEDBACK("process_feedback"),
    TASK_ASSIGNMENT("task_assignment"),
    TRAINING_OF_COPING_SKILLS("training_of_coping_skills");

    private final String code;

    InterventionType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static InterventionType fromCode(String value) {
        if (value == null) return null;
        String v = value.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(it -> it.code.equals(v))
                .findFirst()
                .orElse(null);
    }
}
