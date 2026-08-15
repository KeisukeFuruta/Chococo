package com.chococo.backend.entity;

import java.util.Arrays;

public enum RoastLevel {
    BLONDE("ブロンド"),
    MEDIUM("ミディアム"),
    DARK("ダーク");

    private final String dbValue;

    RoastLevel(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static RoastLevel fromDbValue(String dbValue) {
        return Arrays.stream(values())
                .filter(level -> level.dbValue.equals(dbValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown roast_level: " + dbValue));
    }
}
