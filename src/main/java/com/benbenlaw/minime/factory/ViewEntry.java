package com.benbenlaw.minime.factory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ViewEntry(ItemStack stack, long count) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ViewEntry> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ViewEntry::stack,
            ByteBufCodecs.VAR_LONG, ViewEntry::count,
            ViewEntry::new
    );
}
