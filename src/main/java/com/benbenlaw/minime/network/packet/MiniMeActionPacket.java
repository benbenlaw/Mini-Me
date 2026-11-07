package com.benbenlaw.minime.network.packet;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.UUID;

public record MiniMeActionPacket(UUID miniMe, int action) implements CustomPacketPayload {
    public static final int SIT = 0;
    public static final int CARRY = 1;

    private static final double MAX_DISTANCE = 96.0D;

    public static final Type<MiniMeActionPacket> TYPE = new Type<>(MiniMe.identifier( "mini_me_action"));

    public static final IPayloadHandler<MiniMeActionPacket> HANDLER = (packet, context) -> {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (!(level.getEntity(packet.miniMe()) instanceof MiniMeEntity miniMe) || !miniMe.isAlive()) return;
        if (player.distanceToSqr(miniMe) > MAX_DISTANCE * MAX_DISTANCE) return;

        if (miniMe.getOwnerUUID() != null && !miniMe.getOwnerUUID().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("That Mini Me belongs to someone else."), true);
            return;
        }

        if (packet.action() == SIT) {
            miniMe.toggleSit(player);
        } else if (packet.action() == CARRY) {
            miniMe.toggleCarry(player);
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, MiniMeActionPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, MiniMeActionPacket::miniMe,
            ByteBufCodecs.VAR_INT, MiniMeActionPacket::action,
            MiniMeActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
