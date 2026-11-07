package com.benbenlaw.minime.network.packet;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import com.benbenlaw.minime.screen.FactoryOrdersMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record FactoryOpenPacket(BlockPos pos, boolean orders) implements CustomPacketPayload {
    private static final double MAX_DISTANCE_SQR = 64.0D;

    public static final Type<FactoryOpenPacket> TYPE = new Type<>(MiniMe.identifier( "factory_open"));

    public static final IPayloadHandler<FactoryOpenPacket> HANDLER = (packet, context) -> {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.distanceToSqr(Vec3.atCenterOf(packet.pos())) > MAX_DISTANCE_SQR) return;

        if (player.level().getBlockEntity(packet.pos()) instanceof MiniMeFactoryBlockEntity factory) {
            if (packet.orders()) {
                player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new FactoryOrdersMenu(id, inventory, packet.pos()),
                        factory.getDisplayName()), packet.pos());
            } else {
                player.openMenu(new SimpleMenuProvider(factory, factory.getDisplayName()), packet.pos());
            }
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, FactoryOpenPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FactoryOpenPacket::pos,
            ByteBufCodecs.BOOL, FactoryOpenPacket::orders,
            FactoryOpenPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
