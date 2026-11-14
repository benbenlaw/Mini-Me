package com.benbenlaw.minime.network.packet;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.factory.FactoryRecipeFill;
import com.benbenlaw.minime.screen.MiniMeFactoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.List;

public record FactoryFillPacket(int containerId, List<FactoryRecipeFill.Cell> cells, boolean max) implements CustomPacketPayload {
    public static final Type<FactoryFillPacket> TYPE = new Type<>(MiniMe.identifier("factory_fill"));

    public static final IPayloadHandler<FactoryFillPacket> HANDLER = (packet, context) -> {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof MiniMeFactoryMenu menu
                && menu.containerId == packet.containerId()
                && menu.stillValid(player)) {
            menu.fillFromStorage(player, packet.cells(), packet.max());
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, FactoryFillPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FactoryFillPacket::containerId,
            FactoryRecipeFill.Cell.STREAM_CODEC.apply(ByteBufCodecs.list(9)), FactoryFillPacket::cells,
            ByteBufCodecs.BOOL, FactoryFillPacket::max,
            FactoryFillPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
