package com.benbenlaw.minime.client;

import com.benbenlaw.minime.entity.MiniMeEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MiniMeFakePlayer extends AbstractClientPlayer {

    private final MiniMeEntity entity;

    public MiniMeFakePlayer(ClientLevel level, GameProfile profile, MiniMeEntity entity) {
        super(level, profile);
        this.entity = entity;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}