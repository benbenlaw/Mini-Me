package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
    private static final int MAX_BREAK_TICKS = 400;
    private static final int SCAN_INTERVAL = 40;
    private static final double REACH_SQR = 2.5D * 2.5D;
    private static final double MAX_MINING_RANGE_SQR = 14.0D * 14.0D;

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
        if (!miniMe.isTaskActive(MiniMeTask.MINE)) return false;
        if (miniMe.getHomePos() == null || !miniMe.hasStorageSpace()) return false;
        if (miniMe.shouldDumpMinedItems()) return false;

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
        return target != null && miniMe.isTaskActive(MiniMeTask.MINE) && miniMe.hasStorageSpace()
                && !miniMe.shouldDumpMinedItems();
    }

    @Override
    public void start() {
        breakTime = 0;
        moveToStand();
    }

    @Override
    public void tick() {
        if (horizontalDistanceSqr(standPos, target) > MAX_MINING_RANGE_SQR) {
            BlockPos closer = findStandNear(target);
            if (closer != null) standPos = closer;
        }

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

        int collected = 0;
        for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), miniMe, tool)) {
            collected += drop.getCount();
            ItemStack leftover = miniMe.insertIntoStorage(drop);
            if (!leftover.isEmpty()) miniMe.spawnAtLocation(level, leftover);
        }
        level.destroyBlock(pos, false, miniMe);
        miniMe.noteItemsMined(collected);

        if (MiniMeConfig.DAMAGE_TOOLS.get()) {
            tool.hurtAndBreak(1, miniMe, EquipmentSlot.MAINHAND);
            miniMe.getInventory().setChanged();
        }
    }

    private BlockPos findNext() {
        Level level = miniMe.level();
        MiniMeEntity.Footprint area = miniMe.getQuarryFootprint();

        int topY = Math.min(miniMe.getQuarryTopY(), level.getMaxY());
        int bottomY = Math.max(miniMe.getQuarryBottomY(), level.getMinY());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = Math.min(layerY, topY); y >= bottomY; y--) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;

            for (int x = area.minX(); x < area.maxXExclusive(); x++) {
                for (int z = area.minZ(); z < area.maxZExclusive(); z++) {
                    pos.set(x, y, z);
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
        MiniMeEntity.Footprint area = miniMe.getQuarryFootprint();
        return nearestStandable(area, area.centerX(), area.centerZ(), miniMe.getX(), miniMe.getZ());
    }

    private BlockPos findStandNear(BlockPos block) {
        return nearestStandable(miniMe.getQuarryFootprint(), block.getX(), block.getZ(), block.getX(), block.getZ());
    }

    private BlockPos nearestStandable(MiniMeEntity.Footprint area, int alongX, int alongZ, double refX, double refZ) {
        Level level = miniMe.level();
        int y = miniMe.getHomePos().getY();

        BlockPos[] sides = {
                new BlockPos(area.maxXExclusive(), y, alongZ),
                new BlockPos(area.minX() - 1, y, alongZ),
                new BlockPos(alongX, y, area.maxZExclusive()),
                new BlockPos(alongX, y, area.minZ() - 1)
        };

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos side : sides) {
            BlockPos ground = findStandable(level, side);
            if (ground == null) continue;

            double dx = ground.getX() + 0.5 - refX;
            double dz = ground.getZ() + 0.5 - refZ;
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = ground;
            }
        }
        return best;
    }

    private static BlockPos findStandable(Level level, BlockPos around) {
        for (int dy = 6; dy >= -8; dy--) {
            BlockPos feet = around.above(dy);
            if (level.getBlockState(feet.below()).blocksMotion()
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                return feet;
            }
        }
        return null;
    }

    private static double horizontalDistanceSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
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
