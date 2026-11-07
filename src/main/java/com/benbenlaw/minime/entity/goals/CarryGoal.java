package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class CarryGoal extends Goal {
    private static final double JUMP_RANGE = 2.2D;
    private static final int HOP_TICKS = 8;
    private static final int TIMEOUT = 400;

    private final MiniMeEntity miniMe;
    private int ticks;
    private int hop;

    public CarryGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return miniMe.getPendingCarryPlayer() != null && !miniMe.isCarried();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        ticks = 0;
        hop = -1;
    }

    @Override
    public void tick() {
        Player player = miniMe.getPendingCarryPlayer();
        if (player == null) return;

        miniMe.getLookControl().setLookAt(player, 30.0F, 30.0F);

        if (++ticks > TIMEOUT) {
            miniMe.cancelCarry();
            return;
        }

        if (hop >= 0) {
            if (++hop >= HOP_TICKS) miniMe.completeCarry(player);
            return;
        }

        if (miniMe.distanceTo(player) <= JUMP_RANGE) {
            miniMe.getNavigation().stop();
            miniMe.getJumpControl().jump();
            hop = 0;
        } else if (!miniMe.getNavigation().isInProgress() || ticks % 10 == 0) {
            miniMe.getNavigation().moveTo(player, 1.3D);
        }
    }

    @Override
    public void stop() {
        miniMe.getNavigation().stop();
    }
}
