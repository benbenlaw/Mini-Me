package com.benbenlaw.minime.network.packet;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.screen.MiniMeFactoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record FactoryClickPacket(int containerId, ItemStack key, int button, boolean shift) implements CustomPacketPayload {
    public static final Type<FactoryClickPacket> TYPE = new Type<>(MiniMe.identifier( "factory_click"));

    public static final IPayloadHandler<FactoryClickPacket> HANDLER = (packet, context) -> {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof MiniMeFactoryMenu menu
                && menu.containerId == packet.containerId()
                && menu.stillValid(player)) {
            menu.handleViewClick(player, packet.key(), packet.button(), packet.shift());
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, FactoryClickPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FactoryClickPacket::containerId,
            ItemStack.OPTIONAL_STREAM_CODEC, FactoryClickPacket::key,
            ByteBufCodecs.VAR_INT, FactoryClickPacket::button,
            ByteBufCodecs.BOOL, FactoryClickPacket::shift,
            FactoryClickPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
