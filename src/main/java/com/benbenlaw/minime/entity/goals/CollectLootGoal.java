package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.EnumSet;

public class CollectLootGoal extends Goal {
    private final MiniMeEntity miniMe;
    private ItemEntity targetItem;

    public CollectLootGoal(MiniMeEntity mob) {
        this.miniMe = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!MiniMeConfig.COLLECT_ITEMS.get() || !miniMe.isTaskEnabled(MiniMeTask.COLLECT) || !miniMe.canPickUpLoot()) return false;
        if (miniMe.getTarget() != null) return false; // Don't loot while fighting

        double bestDistance = Double.MAX_VALUE;
        targetItem = null;

        for (ItemEntity item : miniMe.level().getEntitiesOfClass(
                ItemEntity.class,
                miniMe.getBoundingBox().inflate(MiniMeConfig.COLLECT_RADIUS.get())
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
