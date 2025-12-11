package com.benbenlaw.minime.entity.goals;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.EnumSet;

public class CollectLootGoal extends Goal {
    private final PathfinderMob miniMe;
    private ItemEntity targetItem;

    public CollectLootGoal(PathfinderMob mob) {
        this.miniMe = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!miniMe.canPickUpLoot()) return false;
        if (miniMe.getTarget() != null) return false; // Don't loot while fighting

        double bestDistance = Double.MAX_VALUE;
        targetItem = null;

        for (ItemEntity item : miniMe.level().getEntitiesOfClass(
                ItemEntity.class,
                miniMe.getBoundingBox().inflate(10)
        )) {
            if (!item.isAlive()) continue;

            double dist = miniMe.distanceToSqr(item);
            if (dist < bestDistance) {
                bestDistance = dist;
                targetItem = item;
            }
        }

        return targetItem != null;
    }

    @Override
    public void start() {
        miniMe.getNavigation().moveTo(targetItem.getX(), targetItem.getY(), targetItem.getZ(), 0, 1.0D);
    }

    @Override
    public boolean canContinueToUse() {
        return targetItem != null
                && targetItem.isAlive()
                && miniMe.distanceToSqr(targetItem) > 2.0D;
    }

    @Override
    public void tick() {
        if (targetItem != null) {
            miniMe.getNavigation().moveTo(targetItem.getX(), targetItem.getY(), targetItem.getZ(), 0, 1.0D);

        }
    }
}
