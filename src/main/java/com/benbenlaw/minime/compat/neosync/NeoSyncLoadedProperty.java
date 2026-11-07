package com.benbenlaw.minime.compat.neosync;

import com.benbenlaw.minime.compat.NeoSyncCompat;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record NeoSyncLoadedProperty() implements ConditionalItemModelProperty {
    public static final MapCodec<NeoSyncLoadedProperty> MAP_CODEC = MapCodec.unit(new NeoSyncLoadedProperty());

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        return NeoSyncCompat.isLoaded();
    }

    @Override
    public MapCodec<NeoSyncLoadedProperty> type() {
        return MAP_CODEC;
    }
}
