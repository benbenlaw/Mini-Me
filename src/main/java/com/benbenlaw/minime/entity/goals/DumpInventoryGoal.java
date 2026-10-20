package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.EnumSet;

public class DumpInventoryGoal extends Goal {

    private enum Phase { WALKING, OPENING, CLOSING, DONE }

    private static final int OPEN_TIME = 15;
    private static final int CLOSE_TIME = 10;
    private static final int MAX_WALK_TICKS = 200;
    private static final int COOLDOWN = 100;

    private final MiniMeEntity miniMe;

    private Phase phase = Phase.DONE;
    private BlockPos chestPos;
    private boolean chestOpen;
    private int timer;
    private int cooldown;

    public DumpInventoryGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!MiniMeConfig.DUMP_TO_CHEST.get() || !miniMe.isTaskEnabled(MiniMeTask.DUMP)) return false;

        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!(miniMe.findChestNearHome() instanceof ChestBlockEntity)) return false;

        for (int i = MiniMeEntity.FIRST_STORAGE_SLOT; i < miniMe.getInventory().getContainerSize(); i++) {
            if (!miniMe.getInventory().getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        BlockEntity chest = miniMe.findChestNearHome();
        if (chest == null) {
            phase = Phase.DONE;
            return;
        }
        chestPos = chest.getBlockPos();
        phase = Phase.WALKING;
        timer = 0;
        moveToChest();
    }

    @Override
    public boolean canContinueToUse() {
        return phase != Phase.DONE;
    }

    @Override
    public void tick() {
        miniMe.getLookControl().setLookAt(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5);

        switch (phase) {
            case WALKING -> {
                if (miniMe.distanceToSqr(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5) <= 6.25) {
                    miniMe.getNavigation().stop();
                    chestOpen = miniMe.setChestOpen(chestPos, true);
                    timer = 0;
                    phase = Phase.OPENING;
                } else if (++timer > MAX_WALK_TICKS) {
                    phase = Phase.DONE;
                } else if (!miniMe.getNavigation().isInProgress()) {
                    moveToChest();
                }
            }
            case OPENING -> {
                if (++timer >= OPEN_TIME) {
                    miniMe.dumpInventoryToChest();
                    timer = 0;
                    phase = Phase.CLOSING;
                }
            }
            case CLOSING -> {
                if (++timer >= CLOSE_TIME) {
                    phase = Phase.DONE;
                }
            }
            case DONE -> {
            }
        }
    }

    @Override
    public void stop() {
        if (chestOpen) {
            miniMe.setChestOpen(chestPos, false);
            chestOpen = false;
        }
        phase = Phase.DONE;
        cooldown = COOLDOWN;
        miniMe.resetMinedCount(); // whether or not the trip worked, let mining carry on
        miniMe.getNavigation().stop();
    }

    private void moveToChest() {
        miniMe.getNavigation().moveTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5, 1.0D);
    }
}
