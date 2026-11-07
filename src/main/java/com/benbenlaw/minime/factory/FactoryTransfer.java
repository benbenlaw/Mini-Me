package com.benbenlaw.minime.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.Predicate;

public final class FactoryTransfer {
    private FactoryTransfer() {
    }

    public static ResourceHandler<ItemResource> at(Level level, BlockPos pos, Direction side) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null && side != null) {
            handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        }
        return handler;
    }

    public static boolean canMove(ResourceHandler<ItemResource> from, ResourceHandler<ItemResource> to, Predicate<ItemResource> filter, int max) {
        if (from == null || to == null) return false;

        try (Transaction simulation = Transaction.open(null)) {
            return ResourceHandlerUtil.move(from, to, filter, max, simulation) > 0;
        }
    }

    public static int move(ResourceHandler<ItemResource> from, ResourceHandler<ItemResource> to, Predicate<ItemResource> filter, int max) {
        if (from == null || to == null) return 0;

        return ResourceHandlerUtil.move(from, to, filter, max, null);
    }
}
