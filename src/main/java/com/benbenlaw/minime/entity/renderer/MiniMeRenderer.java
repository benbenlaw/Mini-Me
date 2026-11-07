package com.benbenlaw.minime.entity.renderer;

import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class MiniMeRenderer extends LivingEntityRenderer<MiniMeEntity, AvatarRenderState, PlayerModel> {
    private static final float SCALE = 0.48F;
    private static final double SIT_DROP = 0.3D;
    private static final double CARRY_DROP = 0.0D;

    private final PlayerSkinRenderCache skinCache;

    public MiniMeRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.3f);
        this.skinCache = context.getPlayerSkinRenderCache();

        this.addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), part -> new PlayerModel(part, false)),
                context.getEquipmentRenderer()
        ));
        this.addLayer(new PlayerItemInHandLayer<>(this));
    }

    @Override
    public boolean shouldRender(@NotNull MiniMeEntity entity, @NotNull Frustum culler, double camX, double camY, double camZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity.isCarriedBy(minecraft.player) && minecraft.options.getCameraType().isFirstPerson()) return false;

        return super.shouldRender(entity, culler, camX, camY, camZ);
    }

    @Override
    protected boolean shouldShowName(@NotNull MiniMeEntity entity, double distanceToCameraSq) {
        return entity.isCustomNameVisible() && super.shouldShowName(entity, distanceToCameraSq);
    }

    @Override
    public @NotNull AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(@NotNull MiniMeEntity entity, @NotNull AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);

        state.scale *= SCALE;
        state.id = entity.getId();

        state.skin = skinCache.getOrDefault(entity.getProfile()).playerSkin();

        state.rightArmPose = entity.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        state.leftArmPose = entity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;

        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = false;

        if (entity.isCarried() || entity.isSitting()) state.isPassenger = true;

        Player carrier = entity.getCarrier();
        if (carrier != null) {
            float body = Mth.rotLerp(partialTick, carrier.yBodyRotO, carrier.yBodyRot);
            float head = Mth.rotLerp(partialTick, carrier.yHeadRotO, carrier.yHeadRot);
            state.bodyRot = body;
            state.yRot = Mth.wrapDegrees(head - body);
            state.xRot = Mth.lerp(partialTick, carrier.xRotO, carrier.getXRot());
        }

        Vec3 offset = offsetFor(entity, partialTick);
        state.x += offset.x;
        state.y += offset.y;
        state.z += offset.z;

    }

    private static Vec3 offsetFor(MiniMeEntity entity, float partialTick) {
        Player carrier = entity.getCarrier();
        if (carrier != null) {
            Vec3 current = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()),
                    Mth.lerp(partialTick, entity.zo, entity.getZ()));
            return entity.shoulderPosition(carrier, partialTick).subtract(current).add(0.0D, -CARRY_DROP, 0.0D);
        }
        return entity.isSitting() ? new Vec3(0.0D, -SIT_DROP, 0.0D) : Vec3.ZERO;
    }

    @Override
    public @NotNull Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }
}
