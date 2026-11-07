package com.benbenlaw.minime.network.packet;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.screen.MiniMeMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record CustomizePacket(int containerId, int kind, String value) implements CustomPacketPayload {
    public static final int NAME = 0;
    public static final int SKIN = 1;

    public static final Type<CustomizePacket> TYPE = new Type<>(MiniMe.identifier( "customize"));

    public static final IPayloadHandler<CustomizePacket> HANDLER = (packet, context) -> {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof MiniMeMenu menu
                && menu.containerId == packet.containerId()) {
            menu.customize(player, packet.kind(), packet.value());
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomizePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CustomizePacket::containerId,
            ByteBufCodecs.VAR_INT, CustomizePacket::kind,
            ByteBufCodecs.stringUtf8(64), CustomizePacket::value,
            CustomizePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
