package com.soulbuddy.global.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public enum EmotionTag {
    HAPPY,
    SAD,
    ANGRY,
    ANXIOUS,
    HURT,
    EMBARRASSED;

    private static final Map<String, EmotionTag> KOREAN_MAP = Map.ofEntries(
            Map.entry("기쁨", HAPPY),
            Map.entry("슬픔", SAD),
            Map.entry("분노", ANGRY),
            Map.entry("불안", ANXIOUS),
            Map.entry("상처", HURT),
            Map.entry("당황", EMBARRASSED)
    );

    public static EmotionTag fromKorean(String korean) {
        if (korean == null) return null;
        return KOREAN_MAP.get(korean.trim());
    }

    @JsonCreator
    public static EmotionTag fromJson(String value) {
        if (value == null) return null;
        String v = value.trim();
        try {
            return EmotionTag.valueOf(v.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fromKorean(v);
        }
    }
}
