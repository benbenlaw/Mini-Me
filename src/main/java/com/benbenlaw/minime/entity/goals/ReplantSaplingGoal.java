package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Iterator;

public class ReplantSaplingGoal extends Goal {

    private static final int SCAN_INTERVAL = 20;
    private static final int MAX_TICKS = 200;
    private static final double REACH_SQR = 2.5D * 2.5D;

    private final MiniMeEntity miniMe;

    private MiniMeEntity.ReplantSpot target;
    private int scanCooldown;
    private int ticks;

    public ReplantSaplingGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!MiniMeConfig.TREE_FARMING_ENABLED.get() || !MiniMeConfig.REPLANT_SAPLINGS.get()) return false;
        if (!miniMe.isTaskEnabled(MiniMeTask.REPLANT)) return false;
        if (!miniMe.getMainHandItem().is(ItemTags.AXES)) return false;
        if (miniMe.getReplantSpots().isEmpty()) return false;

        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }
        scanCooldown = SCAN_INTERVAL;

        target = findSpot();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && ticks < MAX_TICKS && canPlant(target);
    }

    @Override
    public void start() {
        ticks = 0;
        moveToTarget();
    }

    @Override
    public void tick() {
        ticks++;
        BlockPos pos = target.pos();
        miniMe.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        if (miniMe.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > REACH_SQR) {
            if (!miniMe.getNavigation().isInProgress()) moveToTarget();
            return;
        }

        int slot = findSaplingSlot(target);
        if (slot < 0 || !(miniMe.getInventory().getItem(slot).getItem() instanceof BlockItem blockItem)) {
            target = null;
            return;
        }

        Level level = miniMe.level();
        BlockState sapling = blockItem.getBlock().defaultBlockState();
        level.setBlockAndUpdate(pos, sapling);
        level.playSound(null, pos, sapling.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        miniMe.swing(InteractionHand.MAIN_HAND);

        miniMe.getInventory().getItem(slot).shrink(1);
        miniMe.getInventory().setChanged();

        miniMe.getReplantSpots().remove(target);
        target = null;
    }

    @Override
    public void stop() {
        target = null;
        miniMe.getNavigation().stop();
    }

    private void moveToTarget() {
        BlockPos pos = target.pos();
        miniMe.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0D);
    }

    private MiniMeEntity.ReplantSpot findSpot() {
        MiniMeEntity.ReplantSpot best = null;
        double bestDistance = Double.MAX_VALUE;

        Iterator<MiniMeEntity.ReplantSpot> iterator = miniMe.getReplantSpots().iterator();
        while (iterator.hasNext()) {
            MiniMeEntity.ReplantSpot spot = iterator.next();
            BlockState state = miniMe.level().getBlockState(spot.pos());

            if (state.is(BlockTags.LOGS)) continue; // tree not chopped down yet

            if (!state.isAir() && !state.canBeReplaced()) {
                iterator.remove();
                continue;
            }
            if (!canPlant(spot)) continue;

            double distance = miniMe.distanceToSqr(spot.pos().getX() + 0.5, spot.pos().getY(), spot.pos().getZ() + 0.5);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = spot;
            }
        }
        return best;
    }

    private boolean canPlant(MiniMeEntity.ReplantSpot spot) {
        BlockState state = miniMe.level().getBlockState(spot.pos());
        if (!state.isAir() && !state.canBeReplaced()) return false;

        int slot = findSaplingSlot(spot);
        if (slot < 0) return false;

        return miniMe.getInventory().getItem(slot).getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().canSurvive(miniMe.level(), spot.pos());
    }

    private int findSaplingSlot(MiniMeEntity.ReplantSpot spot) {
        int fallback = -1;
        for (int i = MiniMeEntity.FIRST_STORAGE_SLOT; i < miniMe.getInventory().getContainerSize(); i++) {
            ItemStack stack = miniMe.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(ItemTags.SAPLINGS)) continue;

            if (spot.preferred() != null && stack.is(spot.preferred())) return i;
            if (fallback < 0) fallback = i;
        }
        return fallback;
    }
}
