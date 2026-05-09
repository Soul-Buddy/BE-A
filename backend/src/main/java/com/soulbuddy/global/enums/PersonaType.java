package com.soulbuddy.global.enums;

public enum PersonaType {
    FRIEND,
    COUNSELOR;

    public String characterName() {
        return this == FRIEND ? "포코" : "루미";
    }

    public String displayName() {
        return this == FRIEND ? "친구형" : "상담사형";
    }

    public String aiModelTag() {
        return this == FRIEND ? "HCX-005-FRIEND" : "HCX-005-COUNSELOR";
    }
}
