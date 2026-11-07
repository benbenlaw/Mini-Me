package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SitGoal extends Goal {
    private final MiniMeEntity miniMe;

    public SitGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return miniMe.isSitting() || miniMe.isCarried();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        miniMe.getNavigation().stop();
        miniMe.setJumping(false);
    }
}
