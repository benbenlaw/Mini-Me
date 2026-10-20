package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;

public class SwordAttackGoal extends MeleeAttackGoal {

    private final MiniMeEntity miniMe;

    public SwordAttackGoal(MiniMeEntity miniMe, double speed, boolean follow) {
        super(miniMe, speed, follow);
        this.miniMe = miniMe;
    }

    @Override
    public boolean canUse() {
        if (!MiniMeConfig.COMBAT_ENABLED.get() || !miniMe.isTaskEnabled(MiniMeTask.COMBAT)) return false;

        ItemStack held = miniMe.getMainHandItem();
        boolean hasSword = held.is(ItemTags.SWORDS);
        boolean isInRange = this.miniMe.isWithinHomeDistance(this.miniMe.getHomePos());
        return hasSword && super.canUse() && isInRange;
    }
}