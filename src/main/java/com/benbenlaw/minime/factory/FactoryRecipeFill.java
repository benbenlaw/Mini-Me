package com.benbenlaw.minime.factory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;

public final class FactoryRecipeFill {
    private FactoryRecipeFill() {
    }

    public record Cell(int slot, List<ItemStack> candidates) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Cell> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Cell::slot,
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(64)), Cell::candidates,
                Cell::new
        );
    }

    public record Result(Map<Integer, ItemStack> picks, int sets, List<Integer> missing) {
    }

    private static final class Used {
        final ItemStack key;
        final long available;
        int uses;

        Used(ItemStack key, long available) {
            this.key = key;
            this.available = available;
        }
    }

    public static Result plan(List<Cell> cells, ToLongFunction<ItemStack> available, boolean max) {
        List<Used> used = new ArrayList<>();
        Map<Integer, ItemStack> picks = new HashMap<>();
        List<Integer> missing = new ArrayList<>();

        for (Cell cell : cells) {
            boolean placed = false;
            for (ItemStack candidate : cell.candidates()) {
                if (candidate.isEmpty()) continue;

                Used entry = null;
                for (Used existing : used) {
                    if (ItemStack.isSameItemSameComponents(existing.key, candidate)) {
                        entry = existing;
                        break;
                    }
                }
                long have = entry != null ? entry.available : available.applyAsLong(candidate);
                int taken = entry != null ? entry.uses : 0;
                if (have - taken < 1) continue;

                if (entry == null) {
                    entry = new Used(candidate.copyWithCount(1), have);
                    used.add(entry);
                }
                entry.uses++;
                picks.put(cell.slot(), entry.key);
                placed = true;
                break;
            }
            if (!placed) missing.add(cell.slot());
        }

        int sets = 1;
        if (max && !used.isEmpty()) {
            sets = Integer.MAX_VALUE;
            for (Used entry : used) {
                sets = (int) Math.min(sets, Math.min(entry.available / entry.uses, entry.key.getMaxStackSize()));
            }
            sets = Math.max(1, sets);
        }
        return new Result(picks, sets, missing);
    }
}
