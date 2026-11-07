package com.benbenlaw.minime.factory;

import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FactoryStorage {
    public static final int MAX_ENTRIES = 3000;

    private FactoryStorage() {
    }

    public static Map<ItemResource, Long> aggregate(Level level, MiniMeFactoryBlockEntity factory) {
        Map<ItemResource, Long> totals = new HashMap<>();
        for (BlockPos pos : factory.storages()) {
            ResourceHandler<ItemResource> handler = FactoryTransfer.at(level, pos, null);
            if (handler == null) continue;

            for (int i = 0; i < handler.size(); i++) {
                ItemResource resource = handler.getResource(i);
                long amount = handler.getAmountAsLong(i);
                if (!resource.isEmpty() && amount > 0) totals.merge(resource, amount, Long::sum);
            }
        }
        return totals;
    }

    public static List<ViewEntry> sorted(Map<ItemResource, Long> totals) {
        List<ViewEntry> entries = new ArrayList<>();
        totals.forEach((resource, count) -> entries.add(new ViewEntry(resource.toStack(1), count)));
        entries.sort((a, b) -> a.stack().getHoverName().getString().toLowerCase(Locale.ROOT)
                .compareTo(b.stack().getHoverName().getString().toLowerCase(Locale.ROOT)));
        return entries.size() > MAX_ENTRIES ? new ArrayList<>(entries.subList(0, MAX_ENTRIES)) : entries;
    }

    public static ItemStack extract(Level level, MiniMeFactoryBlockEntity factory, ItemStack key, int amount) {
        int remaining = amount;
        try (Transaction transaction = Transaction.open(null)) {
            for (BlockPos pos : factory.storages()) {
                ResourceHandler<ItemResource> handler = FactoryTransfer.at(level, pos, null);
                if (handler == null) continue;

                for (int i = 0; i < handler.size() && remaining > 0; i++) {
                    ItemResource resource = handler.getResource(i);
                    if (!resource.isEmpty() && ItemStack.isSameItemSameComponents(resource.toStack(), key)) {
                        remaining -= handler.extract(i, resource, remaining, transaction);
                    }
                }
            }
            transaction.commit();
        }

        int taken = amount - remaining;
        return taken > 0 ? key.copyWithCount(taken) : ItemStack.EMPTY;
    }

    public static int insert(Level level, MiniMeFactoryBlockEntity factory, ItemStack stack) {
        if (stack.isEmpty()) return 0;

        ItemResource resource = ItemResource.of(stack);
        int remaining = stack.getCount();
        try (Transaction transaction = Transaction.open(null)) {
            for (BlockPos pos : factory.storages()) {
                ResourceHandler<ItemResource> handler = FactoryTransfer.at(level, pos, null);
                if (handler == null) continue;

                remaining -= handler.insert(resource, remaining, transaction);
                if (remaining <= 0) break;
            }
            transaction.commit();
        }
        return Math.max(0, remaining);
    }
}
