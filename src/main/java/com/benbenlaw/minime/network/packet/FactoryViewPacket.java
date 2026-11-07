package com.benbenlaw.minime.network.packet;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.factory.ViewEntry;
import com.benbenlaw.minime.screen.MiniMeFactoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.List;

public record FactoryViewPacket(int containerId, List<ViewEntry> entries) implements CustomPacketPayload {
    public static final Type<FactoryViewPacket> TYPE = new Type<>(MiniMe.identifier( "factory_view"));

    public static final IPayloadHandler<FactoryViewPacket> HANDLER = (packet, context) -> {
        if (context.player().containerMenu instanceof MiniMeFactoryMenu menu && menu.containerId == packet.containerId()) {
            menu.setView(packet.entries());
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, FactoryViewPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FactoryViewPacket::containerId,
            ViewEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), FactoryViewPacket::entries,
            FactoryViewPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
