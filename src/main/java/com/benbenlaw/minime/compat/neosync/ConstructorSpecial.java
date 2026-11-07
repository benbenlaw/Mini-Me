package com.benbenlaw.minime.compat.neosync;

import com.benbenlaw.minime.compat.NeoSyncCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public record ConstructorSpecial() implements NoDataSpecialModelRenderer.Unbaked {
    public static final MapCodec<ConstructorSpecial> MAP_CODEC = MapCodec.unit(new ConstructorSpecial());

    @Override
    public MapCodec<ConstructorSpecial> type() {
        return MAP_CODEC;
    }

    @Override
    public NoDataSpecialModelRenderer bake(SpecialModelRenderer.BakingContext context) {
        if (NeoSyncCompat.isLoaded()) return NeoSyncConstructorItemRenderer.create(context);

        return new NoDataSpecialModelRenderer() {
            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            }

            @Override
            public void getExtents(Consumer<Vector3fc> output) {
            }
        };
    }
}
