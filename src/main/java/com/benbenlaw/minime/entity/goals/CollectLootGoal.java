package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.EnumSet;

public class CollectLootGoal extends Goal {
    private static final double COLLECT_RADIUS = 10.0D;

    private final MiniMeEntity miniMe;
    private ItemEntity targetItem;

    public CollectLootGoal(MiniMeEntity mob) {
        this.miniMe = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!miniMe.isTaskActive(MiniMeTask.COLLECT) || !miniMe.canPickUpLoot()) return false;
        if (miniMe.getTarget() != null) return false;

        double bestDistance = Double.MAX_VALUE;
        targetItem = null;

        for (ItemEntity item : miniMe.level().getEntitiesOfClass(
                ItemEntity.class,
                miniMe.getBoundingBox().inflate(COLLECT_RADIUS)
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
                && miniMe.isTaskActive(MiniMeTask.COLLECT)
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
