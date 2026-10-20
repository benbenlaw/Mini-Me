package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ChopTreeGoal extends Goal {

    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_HEIGHT = 20;
    private static final int MIN_NATURAL_LEAVES = 4;
    private static final int MIN_BREAK_TICKS = 5;
    private static final int MAX_BREAK_TICKS = 100;
    private static final int SCAN_INTERVAL = 40;
    private static final double REACH_SQR = 3.0D * 3.0D;

    private final MiniMeEntity miniMe;

    private final Deque<BlockPos> logsToChop = new ArrayDeque<>();
    private BlockPos treeBase;
    private BlockPos breaking;
    private int breakTime;
    private int breakTicksNeeded;
    private int scanCooldown;

    public ChopTreeGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!MiniMeConfig.TREE_FARMING_ENABLED.get() || !miniMe.isTaskEnabled(MiniMeTask.CHOP_TREES)) return false;
        if (!miniMe.getMainHandItem().is(ItemTags.AXES)) return false;
        if (miniMe.getHomePos() == null) return false;

        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }
        scanCooldown = SCAN_INTERVAL;

        return findTree();
    }

    @Override
    public boolean canContinueToUse() {
        return !logsToChop.isEmpty() && miniMe.getMainHandItem().is(ItemTags.AXES);
    }

    @Override
    public void start() {
        breakTime = 0;
        moveToBase();
    }

    @Override
    public void tick() {
        if (treeBase == null) return;
        Level level = miniMe.level();

        // Skip logs that were already removed by something else.
        while (!logsToChop.isEmpty() && !level.getBlockState(logsToChop.peekFirst()).is(BlockTags.LOGS)) {
            logsToChop.pollFirst();
        }
        if (logsToChop.isEmpty()) return;

        if (miniMe.distanceToSqr(treeBase.getX() + 0.5, treeBase.getY(), treeBase.getZ() + 0.5) > REACH_SQR) {
            if (!miniMe.getNavigation().isInProgress()) moveToBase();
            return;
        }
        miniMe.getNavigation().stop();

        BlockPos target = logsToChop.peekFirst();
        miniMe.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        if (!target.equals(breaking)) {
            clearBreakProgress();
            breaking = target;
            breakTime = 0;
            breakTicksNeeded = breakTicksFor(level.getBlockState(target), level, target);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        breakTime++;
        if (breakTime % 10 == 1) {
            miniMe.swing(InteractionHand.MAIN_HAND);
        }
        serverLevel.destroyBlockProgress(miniMe.getId(), target, Math.min(9, breakTime * 10 / breakTicksNeeded));

        if (breakTime >= breakTicksNeeded) {
            clearBreakProgress();
            serverLevel.destroyBlock(target, true, miniMe);
            logsToChop.pollFirst();

            // The stack is shared with the inventory, so a broken axe empties its slot too.
            if (MiniMeConfig.DAMAGE_TOOLS.get()) {
                miniMe.getMainHandItem().hurtAndBreak(1, miniMe, EquipmentSlot.MAINHAND);
                miniMe.getInventory().setChanged();
            }
        }
    }

    @Override
    public void stop() {
        clearBreakProgress();
        logsToChop.clear();
        treeBase = null;
        breakTime = 0;
        miniMe.getNavigation().stop();
    }

    private void queueReplants(Level level, List<BlockPos> logs) {
        int lowest = logs.stream().mapToInt(BlockPos::getY).min().orElse(0);

        for (BlockPos log : logs) {
            if (log.getY() != lowest) continue;
            if (!level.getBlockState(log.below()).is(BlockTags.DIRT)) continue;

            miniMe.queueReplant(log, saplingFor(level.getBlockState(log)));
        }
    }

    private static Item saplingFor(BlockState log) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(log.getBlock());
        String name = id.getPath().replace("stripped_", "");

        for (String suffix : new String[]{"_log", "_wood", "_stem", "_hyphae"}) {
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.length() - suffix.length());
                break;
            }
        }

        String sapling = name.equals("mangrove") ? "mangrove_propagule" : name + "_sapling";
        return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), sapling)).orElse(null);
    }

    private int breakTicksFor(BlockState state, Level level, BlockPos pos) {
        float hardness = state.getDestroySpeed(level, pos);
        float speed = Math.max(1.0F, miniMe.getMainHandItem().getDestroySpeed(state));
        int ticks = (int) Math.ceil(hardness * 30.0F / speed);
        return Math.max(MIN_BREAK_TICKS, Math.min(MAX_BREAK_TICKS, ticks));
    }

    private void clearBreakProgress() {
        if (breaking != null && miniMe.level() instanceof ServerLevel serverLevel) {
            serverLevel.destroyBlockProgress(miniMe.getId(), breaking, -1);
        }
        breaking = null;
        breakTime = 0;
    }

    private void moveToBase() {
        miniMe.getNavigation().moveTo(treeBase.getX() + 0.5, treeBase.getY(), treeBase.getZ() + 0.5, 1.0D);
    }

    private boolean findTree() {
        BlockPos home = miniMe.getHomePos();
        Level level = miniMe.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        Set<BlockPos> alreadyChecked = new HashSet<>();
        List<BlockPos> best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                for (int y = -1; y <= SEARCH_HEIGHT; y++) {
                    pos.set(home.getX() + x, home.getY() + y, home.getZ() + z);

                    if (!level.getBlockState(pos).is(BlockTags.LOGS)) continue;
                    if (level.getBlockState(pos.below()).is(BlockTags.LOGS)) continue; // not the bottom of a column

                    BlockPos start = pos.immutable();
                    if (!miniMe.isWithinHomeDistance(start)) continue; // only the base has to be in the working area
                    if (alreadyChecked.contains(start)) continue;

                    List<BlockPos> cluster = collectLogs(level, start);
                    alreadyChecked.add(start);
                    if (cluster == null) continue;
                    alreadyChecked.addAll(cluster);

                    if (!isNaturalTree(level, cluster)) continue;

                    double distance = miniMe.distanceToSqr(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = cluster;
                    }
                }
            }
        }

        if (best == null) return false;

        best.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        logsToChop.clear();
        logsToChop.addAll(best);
        treeBase = best.stream().min(Comparator.comparingInt((BlockPos p) -> p.getY())).orElseThrow();

        if (MiniMeConfig.REPLANT_SAPLINGS.get()) queueReplants(level, best);
        return true;
    }

    private List<BlockPos> collectLogs(Level level, BlockPos start) {
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            logs.add(current);
            if (logs.size() > MiniMeConfig.MAX_TREE_LOGS.get()) return null;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos next = current.offset(dx, dy, dz);
                        if (seen.contains(next)) continue;
                        if (!level.getBlockState(next).is(BlockTags.LOGS)) continue;

                        seen.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        return logs;
    }

    private static boolean isNaturalTree(Level level, List<BlockPos> logs) {
        Set<BlockPos> counted = new HashSet<>();

        for (BlockPos log : logs) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = log.relative(direction);
                BlockState state = level.getBlockState(neighbour);

                if (state.is(BlockTags.LEAVES)
                        && state.hasProperty(LeavesBlock.PERSISTENT)
                        && !state.getValue(LeavesBlock.PERSISTENT)
                        && counted.add(neighbour)
                        && counted.size() >= MIN_NATURAL_LEAVES) {
                    return true;
                }
            }
        }
        return false;
    }
}
