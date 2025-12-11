package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ReturnHomeGoal extends Goal {
    private final MiniMeEntity miniMe;

    public ReturnHomeGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (miniMe.getHomePos() == null) return false;
        return !miniMe.isWithinHomeDistance(miniMe.blockPosition());
    }

    @Override
    public void tick() {
        BlockPos home = miniMe.getHomePos();
        if (home != null) {
            miniMe.getNavigation().moveTo(home.getX(), home.getY(), home.getZ(), 1.0D);
        }
    }
}