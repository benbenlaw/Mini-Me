package com.benbenlaw.minime.entity.renderer;

import com.benbenlaw.minime.entity.MiniMeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class MiniMeRenderer extends HumanoidMobRenderer<MiniMeEntity, PlayerModel<MiniMeEntity>> {

    public MiniMeRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F
        );
    }

    @Override
    public void render(MiniMeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(MiniMeEntity entity) {

        UUID uuid = entity.getOwnerUUID();
        Minecraft mc = Minecraft.getInstance();

        if (uuid == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }

        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);

            if (info != null) {
                PlayerSkin skin = info.getSkin();
                return skin.texture();
            }
        }

        return DefaultPlayerSkin.get(uuid).texture();
    }

    @Override
    protected void scale(MiniMeEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }
}
