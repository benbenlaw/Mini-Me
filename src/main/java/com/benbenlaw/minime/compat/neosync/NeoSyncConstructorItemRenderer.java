package com.benbenlaw.minime.compat.neosync;

import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.client.model.ShellConstructorModel;
import com.breakinblocks.neosync.client.model.SyncModelLayers;
import com.breakinblocks.neosync.client.render.block.entity.AbstractShellContainerBlockEntityRenderer;
import com.breakinblocks.neosync.client.render.block.entity.ShellContainerRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class NeoSyncConstructorItemRenderer implements NoDataSpecialModelRenderer {
    private static final Identifier TEXTURE = NeoSync.locate("textures/block/shell_constructor.png");

    private final ShellConstructorModel model;
    private final ShellContainerRenderState state;

    private NeoSyncConstructorItemRenderer(ShellConstructorModel model) {
        this.model = model;
        this.state = new ShellContainerRenderState();
        this.state.renderBothHalves = true;
        this.state.showInnerParts = true;
        this.state.texture = TEXTURE;
    }

    public static NoDataSpecialModelRenderer create(SpecialModelRenderer.BakingContext context) {
        return new NeoSyncConstructorItemRenderer(new ShellConstructorModel(context.entityModelSet().bakeLayer(SyncModelLayers.SHELL_CONSTRUCTOR)));
    }

    private static void applyTransform(PoseStack poseStack) {
        poseStack.scale(1.0F, 0.5F, 1.0F);
        AbstractShellContainerBlockEntityRenderer.applyMachineTransform(poseStack, Direction.SOUTH);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        poseStack.pushPose();
        applyTransform(poseStack);
        collector.submitModel(model, state, poseStack, TEXTURE, lightCoords, overlayCoords, outlineColor, null);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        applyTransform(poseStack);
        model.setupAnim(state);
        model.root().getExtentsForGui(poseStack, output);
    }
}
