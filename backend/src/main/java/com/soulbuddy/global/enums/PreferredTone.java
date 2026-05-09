package com.soulbuddy.global.enums;

public enum PreferredTone {
    FRIEND,
    COUNSELOR;

    public PersonaType toPersonaType() {
        return this == FRIEND ? PersonaType.FRIEND : PersonaType.COUNSELOR;
    }
}
