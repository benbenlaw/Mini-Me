package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class MineQuarryGoal extends Goal {

    private static final int MIN_BREAK_TICKS = 5;
    private static final int MAX_BREAK_TICKS = 400; // obsidian with a diamond pickaxe is ~190
    private static final int SCAN_INTERVAL = 40;
    private static final double REACH_SQR = 2.5D * 2.5D;

    private final MiniMeEntity miniMe;

    private BlockPos standPos;
    private BlockPos target;
    private BlockPos breaking;
    private int breakTime;
    private int breakTicksNeeded;
    private int layerY;
    private int scanCooldown;

    public MineQuarryGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!MiniMeConfig.MINING_ENABLED.get() || !miniMe.isTaskEnabled(MiniMeTask.MINE)) return false;
        if (!miniMe.getMainHandItem().is(ItemTags.PICKAXES)) return false;
        if (miniMe.getHomePos() == null || !miniMe.hasStorageSpace()) return false;
        if (miniMe.shouldDumpMinedItems()) return false; // go and empty out first

        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }
        scanCooldown = SCAN_INTERVAL;

        standPos = findStandPos();
        if (standPos == null) return false;

        layerY = Integer.MAX_VALUE;
        target = findNext();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && miniMe.getMainHandItem().is(ItemTags.PICKAXES) && miniMe.hasStorageSpace()
                && !miniMe.shouldDumpMinedItems();
    }

    @Override
    public void start() {
        breakTime = 0;
        moveToStand();
    }

    @Override
    public void tick() {
        if (miniMe.distanceToSqr(standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5) > REACH_SQR) {
            if (!miniMe.getNavigation().isInProgress()) moveToStand();
            return;
        }
        miniMe.getNavigation().stop();

        ServerLevel level = (ServerLevel) miniMe.level();
        if (!canMine(level, target)) {
            clearBreakProgress();
            target = findNext();
            return;
        }

        miniMe.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        if (!target.equals(breaking)) {
            clearBreakProgress();
            breaking = target;
            breakTime = 0;
            breakTicksNeeded = breakTicksFor(level.getBlockState(target), level, target);
        }

        breakTime++;
        if (breakTime % 10 == 1) {
            miniMe.swing(InteractionHand.MAIN_HAND);
        }
        level.destroyBlockProgress(miniMe.getId(), target, Math.min(9, breakTime * 10 / breakTicksNeeded));

        if (breakTime >= breakTicksNeeded) {
            clearBreakProgress();
            mine(level, target);
            target = findNext();
        }
    }

    @Override
    public void stop() {
        clearBreakProgress();
        target = null;
        miniMe.getNavigation().stop();
    }

    private void mine(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ItemStack tool = miniMe.getMainHandItem();

        for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), miniMe, tool)) {
            ItemStack leftover = miniMe.insertIntoStorage(drop);
            if (!leftover.isEmpty()) miniMe.spawnAtLocation(level, leftover);
        }
        level.destroyBlock(pos, false, miniMe);
        miniMe.noteBlockMined();

        if (MiniMeConfig.MINING_DAMAGE_TOOLS.get()) {
            tool.hurtAndBreak(1, miniMe, EquipmentSlot.MAINHAND);
            miniMe.getInventory().setChanged();
        }
    }

    private BlockPos findNext() {
        Level level = miniMe.level();
        BlockPos min = miniMe.getQuarryMin();
        int width = miniMe.getQuarryWidth();

        int topY = min.getY();
        int bottomY = Math.max(level.getMinY(), topY - MiniMeConfig.MAX_QUARRY_DEPTH.get() + 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = Math.min(layerY, topY); y >= bottomY; y--) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    pos.set(min.getX() + x, y, min.getZ() + z);
                    if (!canMine(level, pos)) continue;

                    double distance = standPos.distSqr(pos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.immutable();
                    }
                }
            }

            if (best != null) {
                layerY = y;
                return best;
            }
        }

        layerY = bottomY - 1;
        return null;
    }

    private boolean canMine(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0
                && (!state.requiresCorrectToolForDrops() || miniMe.getMainHandItem().isCorrectToolForDrops(state));
    }

    private BlockPos findStandPos() {
        Level level = miniMe.level();
        BlockPos home = miniMe.getHomePos();
        BlockPos min = miniMe.getQuarryMin();
        int width = miniMe.getQuarryWidth();

        int east = min.getX() + width;
        int west = min.getX() - 1;
        int south = min.getZ() + width;
        int north = min.getZ() - 1;

        BlockPos[] sides = {
                new BlockPos(east, home.getY(), home.getZ()),
                new BlockPos(west, home.getY(), home.getZ()),
                new BlockPos(home.getX(), home.getY(), south),
                new BlockPos(home.getX(), home.getY(), north)
        };

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos side : sides) {
            BlockPos ground = findStandable(level, side);
            if (ground == null) continue;

            double distance = miniMe.distanceToSqr(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = ground;
            }
        }
        return best;
    }

    private static BlockPos findStandable(Level level, BlockPos around) {
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos feet = around.above(dy);
            if (level.getBlockState(feet.below()).blocksMotion()
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                return feet;
            }
        }
        return null;
    }

    private int breakTicksFor(BlockState state, Level level, BlockPos pos) {
        float hardness = state.getDestroySpeed(level, pos);
        float speed = Math.max(1.0F, miniMe.getMainHandItem().getDestroySpeed(state));
        int ticks = (int) Math.ceil(hardness * 30.0F / speed);
        return Math.max(MIN_BREAK_TICKS, Math.min(MAX_BREAK_TICKS, ticks));
    }

    private void clearBreakProgress() {
        if (breaking != null && miniMe.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(miniMe.getId(), breaking, -1);
        }
        breaking = null;
        breakTime = 0;
    }

    private void moveToStand() {
        miniMe.getNavigation().moveTo(standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5, 1.0D);
    }
}
