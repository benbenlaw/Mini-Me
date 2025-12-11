package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.EnumSet;

public class DumpInventoryGoal extends Goal {
    private final MiniMeEntity miniMe;

    public DumpInventoryGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !miniMe.getInventory().isEmpty() && miniMe.findChestNearHome() instanceof ChestBlockEntity;
    }

    @Override
    public void start() {
        if (miniMe.findChestNearHome() != null) {
            miniMe.getNavigation().moveTo(miniMe.findChestNearHome().getBlockPos().getX(),
                    miniMe.findChestNearHome().getBlockPos().getY(),
                    miniMe.findChestNearHome().getBlockPos().getZ(),
                    1.0D);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return miniMe.getNavigation().isInProgress();
    }

    @Override
    public void tick() {
        if (miniMe.findChestNearHome() == null) return;

        if (miniMe.blockPosition().closerThan(miniMe.findChestNearHome().getBlockPos(), 2)) {
            miniMe.dumpInventoryToChest();
            miniMe.getNavigation().stop();
        }
    }
}
