package com.benbenlaw.minime.event.client;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.factory.FactoryRegistry;
import com.benbenlaw.minime.entity.MiniMeTask;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

@EventBusSubscriber(modid = MiniMe.MOD_ID, value = Dist.CLIENT)
public class AreaRenderer {
    private static final double RENDER_RANGE = 96;
    private static final double FACTORY_RENDER_RANGE = 48;
    private static final float LINE_WIDTH = 10.0F;

    private static final int HOME_AREA_COLOR = ARGB.colorFromFloat(0.6F, 0.3F, 1.0F, 0.3F);
    private static final int HOME_BLOCK_COLOR = ARGB.colorFromFloat(0.8F, 1.0F, 1.0F, 1.0F);
    private static final int QUARRY_COLOR = ARGB.colorFromFloat(0.7F, 1.0F, 0.6F, 0.1F);
    private static final int CHEST_AREA_COLOR = ARGB.colorFromFloat(0.7F, 0.2F, 0.8F, 1.0F);
    private static final int FACTORY_AREA_COLOR = ARGB.colorFromFloat(0.7F, 1.0F, 1.0F, 1.0F);

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        List<MiniMeEntity> shown = mc.level.getEntitiesOfClass(MiniMeEntity.class,
                new AABB(camera, camera).inflate(RENDER_RANGE), MiniMeEntity::isShowingArea);
        if (shown.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffer.getBuffer(RenderTypes.lines());

        for (MiniMeEntity miniMe : shown) {
            BlockPos home = miniMe.getHomePos();
            if (home == null) continue;

            box(poseStack, lines, camera, new AABB(home).inflate(miniMe.getHomeRadius()), HOME_AREA_COLOR);
            box(poseStack, lines, camera, new AABB(home), HOME_BLOCK_COLOR);

            if (miniMe.isTaskActive(MiniMeTask.MINE)) {
                MiniMeEntity.Footprint quarry = miniMe.getQuarryFootprint();
                box(poseStack, lines, camera, new AABB(
                        quarry.minX(), miniMe.getQuarryBottomY(), quarry.minZ(),
                        quarry.maxXExclusive(), miniMe.getQuarryTopY() + 1, quarry.maxZExclusive()), QUARRY_COLOR);
            }

            if (miniMe.isTaskActive(MiniMeTask.DUMP)) {
                MiniMeEntity.Footprint chests = miniMe.getChestFootprint();
                box(poseStack, lines, camera, new AABB(
                        chests.minX(), home.getY() - 4, chests.minZ(),
                        chests.maxXExclusive(), home.getY() + 5, chests.maxZExclusive()), CHEST_AREA_COLOR);
            }
        }

        buffer.endBatch();
    }

    @SubscribeEvent
    public static void onRenderFactories(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        List<MiniMeFactoryBlockEntity> factories = FactoryRegistry.within(mc.level, mc.player.blockPosition(), FACTORY_RENDER_RANGE)
                .stream().filter(MiniMeFactoryBlockEntity::isShowingArea).toList();
        if (factories.isEmpty()) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffer.getBuffer(RenderTypes.lines());

        for (MiniMeFactoryBlockEntity factory : factories) {
            box(poseStack, lines, camera, factory.getArea(), FACTORY_AREA_COLOR);
        }

        buffer.endBatch();
    }

    private static void box(PoseStack poseStack, VertexConsumer lines, Vec3 camera, AABB box, int color) {
        AABB shifted = box.move(-camera.x, -camera.y, -camera.z);
        ShapeRenderer.renderShape(poseStack, lines, Shapes.create(shifted), 0, 0, 0, color, LINE_WIDTH);
    }
}
