package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SwordAttackGoal extends MeleeAttackGoal {

    private final MiniMeEntity miniMe;

    public SwordAttackGoal(MiniMeEntity miniMe, double speed, boolean follow) {
        super(miniMe, speed, follow);
        this.miniMe = miniMe;
    }

    @Override
    public boolean canUse() {
        ItemStack held = miniMe.getMainHandItem();
        boolean hasSword = held.getItem() == Items.DIAMOND_SWORD || held.getItem() == Items.WOODEN_SWORD;
       // boolean isInRange = this.miniMe.isWithinHomeDistance(this.miniMe.getHomePos());
        return hasSword && super.canUse();// && isInRange;
    }
}