package com.benbenlaw.minime.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum MiniMeTask implements StringRepresentable {
    COMBAT("Fight"),
    CHOP_TREES("Chop trees"),
    REPLANT("Replant"),
    SHEAR("Shear"),
    MINE("Mine"),
    COLLECT("Collect"),
    DUMP("Use chest"),
    FACTORY("Factory");

    public static final Codec<MiniMeTask> CODEC = StringRepresentable.fromEnum(MiniMeTask::values);

    private final String label;

    MiniMeTask(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean hasSettings() {
        return this == MINE || this == DUMP;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
