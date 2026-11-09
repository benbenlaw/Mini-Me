package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class SwordAttackGoal extends MeleeAttackGoal {
    private final MiniMeEntity miniMe;

    public SwordAttackGoal(MiniMeEntity miniMe, double speed, boolean follow) {
        super(miniMe, speed, follow);
        this.miniMe = miniMe;
    }

    @Override
    public boolean canUse() {
        if (!miniMe.isTaskActive(MiniMeTask.COMBAT)) return false;

        boolean isInRange = this.miniMe.isWithinHomeDistance(this.miniMe.getHomePos());
        return super.canUse() && isInRange;
    }
}
