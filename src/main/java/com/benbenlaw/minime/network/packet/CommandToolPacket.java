package com.benbenlaw.minime.network.packet;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.entity.AreaTarget;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.item.CommandToolItem;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.UUID;

public record CommandToolPacket(UUID miniMe, int area) implements CustomPacketPayload {
    private static final double MAX_DISTANCE = 96.0D;

    public static final Type<CommandToolPacket> TYPE = new Type<>(MiniMe.identifier( "command_tool"));

    public static final IPayloadHandler<CommandToolPacket> HANDLER = (packet, context) -> {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;

        AreaTarget[] areas = AreaTarget.values();
        if (packet.area() < 0 || packet.area() >= areas.length) return;

        if (level.getEntity(packet.miniMe()) instanceof MiniMeEntity miniMe
                && miniMe.isAlive()
                && player.distanceToSqr(miniMe) <= MAX_DISTANCE * MAX_DISTANCE) {
            CommandToolItem.bind(player, miniMe, areas[packet.area()]);
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, CommandToolPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CommandToolPacket::miniMe,
            ByteBufCodecs.VAR_INT, CommandToolPacket::area,
            CommandToolPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
