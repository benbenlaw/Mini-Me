package com.benbenlaw.minime.compat.neosync;

import com.benbenlaw.minime.block.MiniMeConstructorBlock;
import com.benbenlaw.minime.block.entity.MiniMeConstructorBlockEntity;
import com.benbenlaw.minime.entity.MMEntities;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.breakinblocks.neosync.NeoSync;
import com.breakinblocks.neosync.client.model.ShellConstructorModel;
import com.breakinblocks.neosync.client.model.ShellVoxelModel;
import com.breakinblocks.neosync.client.model.SyncModelLayers;
import com.breakinblocks.neosync.client.render.block.entity.AbstractShellContainerBlockEntityRenderer;
import com.breakinblocks.neosync.client.render.block.entity.ShellContainerRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class NeoSyncConstructorRenderer implements BlockEntityRenderer<MiniMeConstructorBlockEntity, ShellContainerRenderState> {
    private static final Identifier TEXTURE = NeoSync.locate("textures/block/shell_constructor.png");
    private static final Identifier VOXEL_TEXTURE = AbstractShellContainerBlockEntityRenderer.VOXEL_TEXTURE;
    private static final float MINI_ME_SCALE = 0.48F * 0.9F;
    private static final float ENTITY_VISIBLE_AT = 0.75F;
    private static final float FLOOR = 0.04F;

    private final ShellConstructorModel model;
    private final ShellVoxelModel voxelModel;
    private final Map<MiniMeConstructorBlockEntity, MiniMeEntity> dummies = new WeakHashMap<>();

    public NeoSyncConstructorRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new ShellConstructorModel(context.bakeLayer(SyncModelLayers.SHELL_CONSTRUCTOR));
        this.voxelModel = new ShellVoxelModel(context.bakeLayer(ModelLayers.PLAYER));
    }

    @Override
    public ShellContainerRenderState createRenderState() {
        return new ShellContainerRenderState();
    }

    @Override
    public void extractRenderState(MiniMeConstructorBlockEntity blockEntity, ShellContainerRenderState renderState, float partialTick,
                                   Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);

        BlockState blockState = blockEntity.getBlockState();
        float progress = blockEntity.isReady() ? 1.0F : blockEntity.getShownProgress(partialTick);
        boolean building = blockEntity.isBuilding();
        boolean ready = blockEntity.isReady();

        renderState.facing = blockState.getValue(MiniMeConstructorBlock.FACING);
        renderState.isLowerHalf = true;
        renderState.renderBothHalves = true;
        renderState.texture = TEXTURE;
        renderState.doorOpenProgress = blockEntity.getDoorOpenProgress(partialTick);
        renderState.shellProgress = progress;
        renderState.hasShell = building || ready;
        renderState.showInnerParts = true;

        renderState.shellEntityState = null;
        if (ready || (building && progress >= ENTITY_VISIBLE_AT)) {
            MiniMeEntity dummy = dummyFor(blockEntity);
            if (dummy != null) {
                float yaw = renderState.facing.getOpposite().toYRot();
                dummy.setPos(blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY(), blockEntity.getBlockPos().getZ() + 0.5);
                dummy.setYRot(yaw);
                dummy.yRotO = yaw;
                dummy.yBodyRot = yaw;
                dummy.yBodyRotO = yaw;
                dummy.yHeadRot = yaw;
                dummy.yHeadRotO = yaw;
                EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                renderState.shellEntityState = dispatcher.extractEntity(dummy, partialTick);
            }
        }
    }

    @Nullable
    private MiniMeEntity dummyFor(MiniMeConstructorBlockEntity blockEntity) {
        if (blockEntity.getLevel() == null) return null;

        MiniMeEntity dummy = dummies.get(blockEntity);
        UUID owner = blockEntity.getOwner();
        if (dummy == null) {
            dummy = MMEntities.MINI_ME.get().create(blockEntity.getLevel(), EntitySpawnReason.LOAD);
            if (dummy == null) return null;

            dummy.setOwnerUUID(owner);
            dummies.put(blockEntity, dummy);
        } else if (owner != null && !owner.equals(dummy.getOwnerUUID())) {
            dummy.setOwnerUUID(owner);
        }
        return dummy;
    }

    @Override
    public void submit(ShellContainerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(1.0F, 0.5F, 1.0F);
        AbstractShellContainerBlockEntityRenderer.applyMachineTransform(poseStack, state.facing);
        poseStack.pushPose();
        collector.submitModel(model, state, poseStack, state.texture, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
        poseStack.popPose();
        poseStack.popPose();

        if (state.hasShell && state.shellProgress < 1.0F) {
            poseStack.pushPose();
            poseStack.translate(0.5, FLOOR, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(facingYaw(state.facing) + 180.0F));
            poseStack.scale(-MINI_ME_SCALE, -MINI_ME_SCALE, MINI_ME_SCALE);
            poseStack.translate(0.0, -1.501, 0.0);
            collector.submitModel(voxelModel, state, poseStack, VOXEL_TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
            poseStack.popPose();
        }

        if (state.shellEntityState != null) {
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            poseStack.pushPose();
            poseStack.translate(0.5, FLOOR, 0.5);
            poseStack.scale(0.9F, 0.9F, 0.9F);
            dispatcher.submit(state.shellEntityState, camera, 0.0, 0.0, 0.0, poseStack, collector);
            poseStack.popPose();
        }
    }

    private static float facingYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}
