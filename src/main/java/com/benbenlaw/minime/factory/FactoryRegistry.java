package com.benbenlaw.minime.factory;

import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class FactoryRegistry {
    private static final Map<Level, Set<BlockPos>> FACTORIES = new WeakHashMap<>();

    private FactoryRegistry() {
    }

    public static synchronized void add(Level level, BlockPos pos) {
        FACTORIES.computeIfAbsent(level, key -> new HashSet<>()).add(pos.immutable());
    }

    public static synchronized void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = FACTORIES.get(level);
        if (set != null) set.remove(pos);
    }

    public static synchronized MiniMeFactoryBlockEntity nearest(Level level, BlockPos center, double range) {
        Set<BlockPos> set = FACTORIES.get(level);
        if (set == null) return null;

        MiniMeFactoryBlockEntity best = null;
        double bestDistance = range * range;
        for (BlockPos pos : set) {
            if (!(level.getBlockEntity(pos) instanceof MiniMeFactoryBlockEntity factory)) continue;

            double distance = pos.distSqr(center);
            if (distance <= bestDistance) {
                best = factory;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static synchronized java.util.List<MiniMeFactoryBlockEntity> within(Level level, BlockPos center, double range) {
        java.util.List<MiniMeFactoryBlockEntity> result = new java.util.ArrayList<>();
        Set<BlockPos> set = FACTORIES.get(level);
        if (set == null) return result;

        for (BlockPos pos : set) {
            if (pos.distSqr(center) <= range * range && level.getBlockEntity(pos) instanceof MiniMeFactoryBlockEntity factory) {
                result.add(factory);
            }
        }
        return result;
    }
}
