package com.benbenlaw.minime.entity;

import com.benbenlaw.minime.config.MiniMeConfig;

public enum MiniMeSetting {
    QUARRY_SIZE_X,
    QUARRY_SIZE_Z,
    QUARRY_OFFSET_X,
    QUARRY_OFFSET_Z,
    MIN_DEPTH,
    MAX_DEPTH,
    DUMP_AFTER,

    CHEST_SIZE_X,
    CHEST_SIZE_Z,
    CHEST_OFFSET_X,
    CHEST_OFFSET_Z;

    private static final int MAX_DUMP_AFTER = 512;
    private static final int DEFAULT_DUMP_AFTER = 16;
    private static final int MAX_AREA_OFFSET = 32;
    private static final int MAX_CHEST_AREA_SIZE = 33;
    private static final int MAX_QUARRY_WIDTH = 15;
    private static final int DEPTH_LIMIT = 4095;

    public int step() {
        return this == DUMP_AFTER ? 8 : 1;
    }

    private boolean isSize() {
        return name().contains("_SIZE_");
    }

    private boolean isOffset() {
        return name().contains("_OFFSET_");
    }

    public int min() {
        if (isSize()) return 1;
        if (isOffset()) return -MAX_AREA_OFFSET;
        return switch (this) {
            case DUMP_AFTER -> 0;
            default -> -DEPTH_LIMIT;
        };
    }

    public int max() {
        if (name().startsWith("CHEST_") && isSize()) return MAX_CHEST_AREA_SIZE;
        if (isSize()) return MAX_QUARRY_WIDTH;
        if (isOffset()) return MAX_AREA_OFFSET;
        return switch (this) {
            case DUMP_AFTER -> MAX_DUMP_AFTER;
            default -> DEPTH_LIMIT;
        };
    }

    public int defaultValue() {
        int value;
        if (name().startsWith("CHEST_") && isSize()) {
            value = 2 * MiniMeConfig.DEFAULT_HOME_RADIUS.get() + 1;
        } else if (isSize()) {
            value = 5;
        } else if (isOffset()) {
            value = 0;
        } else {
            value = switch (this) {
                case MIN_DEPTH -> 1;
                case MAX_DEPTH -> 16;
                default -> DEFAULT_DUMP_AFTER;
            };
        }
        return clamp(value);
    }

    public int clamp(int value) {
        return Math.max(min(), Math.min(value, max()));
    }
}
