package com.benbenlaw.minime.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum AreaTarget implements StringRepresentable {
    QUARRY("Quarry", MiniMeSetting.QUARRY_OFFSET_X, MiniMeSetting.QUARRY_OFFSET_Z),
    CHEST("Chest search area", MiniMeSetting.CHEST_OFFSET_X, MiniMeSetting.CHEST_OFFSET_Z);

    public static final Codec<AreaTarget> CODEC = StringRepresentable.fromEnum(AreaTarget::values);

    public final String label;
    public final MiniMeSetting offsetX;
    public final MiniMeSetting offsetZ;

    AreaTarget(String label, MiniMeSetting offsetX, MiniMeSetting offsetZ) {
        this.label = label;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
