package com.benbenlaw.minime.item;

import com.benbenlaw.minime.entity.AreaTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record CommandTarget(UUID miniMe, AreaTarget area) {
    public static final Codec<CommandTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("mini_me").forGetter(CommandTarget::miniMe),
            AreaTarget.CODEC.fieldOf("area").forGetter(CommandTarget::area)
    ).apply(instance, CommandTarget::new));

    public static final StreamCodec<ByteBuf, CommandTarget> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CommandTarget::miniMe,
            ByteBufCodecs.VAR_INT.map(index -> AreaTarget.values()[index], AreaTarget::ordinal), CommandTarget::area,
            CommandTarget::new);
}
