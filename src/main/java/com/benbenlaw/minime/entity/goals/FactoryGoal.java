package com.benbenlaw.minime.entity.goals;

import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import com.benbenlaw.minime.factory.FactoryPlan;
import com.benbenlaw.minime.factory.FactoryRegistry;
import com.benbenlaw.minime.factory.FactoryStatus;
import com.benbenlaw.minime.factory.FactoryTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class FactoryGoal extends Goal {
    private enum Phase { TO_SOURCE, TO_DESTINATION, DONE }

    private sealed interface Job permits Trip, Craft {
    }

    private record Trip(BlockPos from, Direction fromSide, Predicate<ItemResource> filter, int max, BlockPos to, Direction toSide) implements Job {
    }

    private record Craft(FactoryPlan plan) implements Job {
    }

    private static final double RANGE = 24.0D;
    private static final int ACTION_DELAY = 12;
    private static final double REACH_SQR = 6.25D;
    private static final int MAX_WALK_TICKS = 400;
    private static final int TELEPORT_AFTER = 50;
    private static final int COOLDOWN_BUSY = 5;
    private static final int COOLDOWN_IDLE = 20;

    private final MiniMeEntity miniMe;

    private MiniMeFactoryBlockEntity factory;
    private Job job;
    private Phase phase = Phase.DONE;
    private BlockPos target;
    private int walkTicks;
    private int stuckTicks;
    private double lastDistance;
    private int actionTimer;
    private int cooldown;
    private boolean acted;

    public FactoryGoal(MiniMeEntity miniMe) {
        this.miniMe = miniMe;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!miniMe.isTaskActive(MiniMeTask.FACTORY) || miniMe.getHomePos() == null) return false;

        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!(miniMe.level() instanceof ServerLevel level)) return false;

        MiniMeFactoryBlockEntity found = FactoryRegistry.nearest(level, miniMe.getHomePos(), RANGE);
        if (found == null) {
            cooldown = COOLDOWN_IDLE * 2;
            return false;
        }
        if (!found.tryClaim(miniMe.getUUID(), level.getGameTime())) {
            cooldown = COOLDOWN_IDLE;
            return false;
        }

        factory = found;
        Job next = choose(level);
        if (next == null) {
            cooldown = COOLDOWN_IDLE;
            return false;
        }
        job = next;
        return true;
    }

    @Override
    public void start() {
        acted = false;
        walkTicks = 0;
        stuckTicks = 0;
        actionTimer = 0;
        lastDistance = Double.MAX_VALUE;

        if (job instanceof Trip trip) {
            if (miniMe.getFactoryCarry().isEmpty()) {
                phase = Phase.TO_SOURCE;
                target = trip.from();
            } else {
                phase = Phase.TO_DESTINATION;
                target = trip.to();
            }
        } else {
            phase = Phase.TO_SOURCE;
            target = factory.getBlockPos();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return phase != Phase.DONE && !factory.isRemoved() && miniMe.isTaskActive(MiniMeTask.FACTORY);
    }

    @Override
    public void tick() {
        if (!(miniMe.level() instanceof ServerLevel level)) return;

        factory.tryClaim(miniMe.getUUID(), level.getGameTime());
        factory.setStatus(FactoryStatus.WORKING);
        miniMe.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        double distance = miniMe.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (distance > REACH_SQR) {
            approach(level, distance);
            return;
        }

        miniMe.getNavigation().stop();
        if (++actionTimer < ACTION_DELAY) return;
        actionTimer = 0;

        if (job instanceof Craft craft) {
            if (craft(level, craft.plan())) acted = true;
            phase = Phase.DONE;
        } else if (job instanceof Trip trip) {
            if (phase == Phase.TO_SOURCE) {
                pickUp(level, trip);
                if (miniMe.getFactoryCarry().isEmpty()) {
                    phase = Phase.DONE;
                } else {
                    phase = Phase.TO_DESTINATION;
                    target = trip.to();
                    walkTicks = 0;
                    stuckTicks = 0;
                    lastDistance = Double.MAX_VALUE;
                }
            } else {
                dropOff(level, trip);
                acted = true;
                phase = Phase.DONE;
            }
        }
    }

    @Override
    public void stop() {
        phase = Phase.DONE;
        cooldown = acted ? COOLDOWN_BUSY : COOLDOWN_IDLE;
        miniMe.getNavigation().stop();
    }

    private void approach(ServerLevel level, double distance) {
        if (++walkTicks > MAX_WALK_TICKS) {
            phase = Phase.DONE;
            return;
        }

        if (distance < lastDistance - 0.01D) {
            lastDistance = distance;
            stuckTicks = 0;
        } else if (++stuckTicks > TELEPORT_AFTER) {
            teleportNear(level, target);
            stuckTicks = 0;
            lastDistance = Double.MAX_VALUE;
            return;
        }

        if (!miniMe.getNavigation().isInProgress()) {
            miniMe.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0D);
        }
    }

    private void teleportNear(ServerLevel level, BlockPos pos) {
        for (int dy = 0; dy >= -1; dy--) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos feet = pos.offset(dx, dy, dz);
                    if (feet.equals(pos)) continue;
                    if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) continue;
                    if (!level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
                    if (level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) continue;

                    miniMe.getNavigation().stop();
                    miniMe.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                    return;
                }
            }
        }
        phase = Phase.DONE;
    }

    private Job choose(ServerLevel level) {
        List<BlockPos> storages = factory.storages();

        if (!miniMe.getFactoryCarry().isEmpty()) return recover(level, storages);

        List<MiniMeFactoryBlockEntity.Order> orders = factory.getOrders();
        if (orders.isEmpty()) {
            factory.setStatus(FactoryStatus.NO_ORDERS);
            factory.setActive("");
            return null;
        }
        if (storages.isEmpty()) {
            factory.setStatus(FactoryStatus.NO_STORAGE);
            return null;
        }

        List<Integer> stocks = new ArrayList<>();
        for (MiniMeFactoryBlockEntity.Order order : orders) stocks.add(count(level, storages, order.item()));
        factory.setStocks(stocks);

        for (MiniMeFactoryBlockEntity.Order order : orders) {
            for (FactoryPlan plan : factory.plans(level, order.item())) {
                if (!plan.kind().cooking()) continue;

                Job collect = collectJob(level, plan, machines(level, plan.kind()), storages);
                if (collect != null) {
                    factory.setActive(plan.label());
                    return collect;
                }
            }
        }

        boolean anyNeeded = false;
        boolean anyRecipe = false;
        boolean anyMachine = false;
        for (int i = 0; i < orders.size(); i++) {
            MiniMeFactoryBlockEntity.Order order = orders.get(i);
            if (stocks.get(i) >= order.amount()) continue;
            anyNeeded = true;

            for (FactoryPlan plan : factory.plans(level, order.item())) {
                anyRecipe = true;
                Job next;
                if (plan.kind().cooking()) {
                    List<BlockPos> machines = machines(level, plan.kind());
                    anyMachine |= !machines.isEmpty();
                    next = feedJob(level, plan, machines, storages);
                } else {
                    anyMachine = true;
                    next = craftJob(level, plan, storages);
                }
                if (next != null) {
                    factory.setActive(plan.label());
                    return next;
                }
            }
        }

        factory.setActive("");
        if (!anyNeeded) {
            factory.setStatus(FactoryStatus.STOCKED);
        } else if (!anyRecipe) {
            factory.setStatus(FactoryStatus.NO_RECIPE);
        } else {
            factory.setStatus(anyMachine ? FactoryStatus.WAITING : FactoryStatus.NO_MACHINE);
        }
        return null;
    }

    private int count(ServerLevel level, List<BlockPos> storages, ItemStack item) {
        long total = 0;
        for (BlockPos storage : storages) {
            ResourceHandler<ItemResource> handler = FactoryTransfer.at(level, storage, null);
            if (handler == null) continue;
            for (int i = 0; i < handler.size(); i++) {
                ItemResource resource = handler.getResource(i);
                if (!resource.isEmpty() && ItemStack.isSameItemSameComponents(resource.toStack(), item)) {
                    total += handler.getAmountAsLong(i);
                }
            }
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    private Job recover(ServerLevel level, List<BlockPos> storages) {
        ItemStack carried = miniMe.getFactoryCarry();
        Predicate<ItemResource> same = resource -> ItemStack.isSameItemSameComponents(resource.toStack(), carried);

        for (BlockPos destination : storages) {
            SimpleContainer holder = new SimpleContainer(carried.copy());
            if (FactoryTransfer.canMove(VanillaContainerWrapper.of(holder), FactoryTransfer.at(level, destination, null), same, carried.getCount())) {
                return new Trip(destination, null, same, carried.getCount(), destination, null);
            }
        }

        Containers.dropItemStack(level, miniMe.getX(), miniMe.getY(), miniMe.getZ(), carried);
        miniMe.setFactoryCarry(ItemStack.EMPTY);
        return null;
    }

    private List<BlockPos> machines(ServerLevel level, FactoryPlan.Kind kind) {
        List<BlockPos> machines = new ArrayList<>();
        for (BlockPos pos : factory.machines()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null || entity instanceof RandomizableContainerBlockEntity) continue;
            if (fits(kind, level.getBlockState(pos))) machines.add(pos);
        }
        machines.sort((a, b) -> Double.compare(a.distSqr(factory.getBlockPos()), b.distSqr(factory.getBlockPos())));
        return machines;
    }

    private static boolean fits(FactoryPlan.Kind kind, BlockState state) {
        if (!(state.getBlock() instanceof AbstractFurnaceBlock)) return true;

        return switch (kind) {
            case SMELT -> state.getBlock() instanceof FurnaceBlock;
            case BLAST -> state.getBlock() instanceof BlastFurnaceBlock;
            case SMOKE -> state.getBlock() instanceof SmokerBlock;
            default -> false;
        };
    }

    private Job collectJob(ServerLevel level, FactoryPlan plan, List<BlockPos> machines, List<BlockPos> storages) {
        Predicate<ItemResource> resultFilter = resource -> ItemStack.isSameItemSameComponents(resource.toStack(), plan.result());

        for (BlockPos machine : machines) {
            ResourceHandler<ItemResource> machineOut = FactoryTransfer.at(level, machine, Direction.DOWN);
            for (BlockPos storage : storages) {
                if (FactoryTransfer.canMove(machineOut, FactoryTransfer.at(level, storage, null), resultFilter, 64)) {
                    return new Trip(machine, Direction.DOWN, resultFilter, 64, storage, null);
                }
            }
        }
        return null;
    }

    private Job feedJob(ServerLevel level, FactoryPlan plan, List<BlockPos> machines, List<BlockPos> storages) {
        Ingredient ingredient = plan.ingredients().getFirst();
        Predicate<ItemResource> ingredientFilter = resource -> ingredient.test(resource.toStack());
        Predicate<ItemResource> fuelFilter = resource -> level.fuelValues().isFuel(resource.toStack());

        for (BlockPos machine : machines) {
            BlockEntity entity = level.getBlockEntity(machine);
            boolean furnace = entity instanceof AbstractFurnaceBlockEntity;
            AbstractFurnaceBlockEntity furnaceEntity = furnace ? (AbstractFurnaceBlockEntity) entity : null;

            if (furnace && !furnaceEntity.getItem(0).isEmpty() && furnaceEntity.getItem(1).getCount() < 4) {
                ResourceHandler<ItemResource> fuelSlot = FactoryTransfer.at(level, machine, Direction.NORTH);
                for (BlockPos storage : storages) {
                    if (FactoryTransfer.canMove(FactoryTransfer.at(level, storage, null), fuelSlot, fuelFilter, 4)) {
                        return new Trip(storage, null, fuelFilter, 4, machine, Direction.NORTH);
                    }
                }
            }

            if (furnace && furnaceEntity.getItem(0).getCount() >= 8) continue;

            ResourceHandler<ItemResource> machineIn = FactoryTransfer.at(level, machine, Direction.UP);
            for (BlockPos storage : storages) {
                if (FactoryTransfer.canMove(FactoryTransfer.at(level, storage, null), machineIn, ingredientFilter, 16)) {
                    return new Trip(storage, null, ingredientFilter, 16, machine, Direction.UP);
                }
            }
        }
        return null;
    }

    private Job craftJob(ServerLevel level, FactoryPlan plan, List<BlockPos> storages) {
        List<BlockPos> inputs = storages;
        List<BlockPos> outputs = storages;

        record Stock(ItemStack stack, int[] remaining) {
        }

        List<Stock> stocks = new ArrayList<>();
        for (BlockPos input : inputs) {
            ResourceHandler<ItemResource> handler = FactoryTransfer.at(level, input, null);
            if (handler == null) continue;
            for (int i = 0; i < handler.size(); i++) {
                ItemResource resource = handler.getResource(i);
                long amount = handler.getAmountAsLong(i);
                if (!resource.isEmpty() && amount > 0) {
                    stocks.add(new Stock(resource.toStack(), new int[]{(int) Math.min(amount, Integer.MAX_VALUE)}));
                }
            }
        }

        for (Ingredient ingredient : plan.ingredients()) {
            boolean found = false;
            for (Stock stock : stocks) {
                if (stock.remaining()[0] > 0 && ingredient.test(stock.stack())) {
                    stock.remaining()[0]--;
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }

        ItemResource result = ItemResource.of(plan.result());
        int count = plan.result().getCount();
        for (BlockPos output : outputs) {
            ResourceHandler<ItemResource> handler = FactoryTransfer.at(level, output, null);
            if (handler == null) continue;
            try (Transaction simulation = Transaction.open(null)) {
                if (handler.insert(result, count, simulation) == count) return new Craft(plan);
            }
        }
        return null;
    }

    private boolean craft(ServerLevel level, FactoryPlan plan) {
        List<BlockPos> inputs = factory.storages();
        List<BlockPos> outputs = inputs;

        try (Transaction transaction = Transaction.open(null)) {
            for (Ingredient ingredient : plan.ingredients()) {
                boolean took = false;
                for (BlockPos input : inputs) {
                    ResourceHandler<ItemResource> handler = FactoryTransfer.at(level, input, null);
                    if (handler == null) continue;

                    for (int i = 0; i < handler.size() && !took; i++) {
                        ItemResource resource = handler.getResource(i);
                        if (!resource.isEmpty() && ingredient.test(resource.toStack()) && handler.extract(i, resource, 1, transaction) == 1) {
                            took = true;
                        }
                    }
                    if (took) break;
                }
                if (!took) return false;
            }

            ItemResource result = ItemResource.of(plan.result());
            int count = plan.result().getCount();
            for (BlockPos output : outputs) {
                ResourceHandler<ItemResource> handler = FactoryTransfer.at(level, output, null);
                if (handler == null) continue;

                try (Transaction attempt = Transaction.open(transaction)) {
                    if (handler.insert(result, count, attempt) == count) {
                        attempt.commit();
                        transaction.commit();
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void pickUp(ServerLevel level, Trip trip) {
        SimpleContainer holder = new SimpleContainer(miniMe.getFactoryCarry().copy());
        FactoryTransfer.move(FactoryTransfer.at(level, trip.from(), trip.fromSide()), VanillaContainerWrapper.of(holder), trip.filter(), trip.max());
        miniMe.setFactoryCarry(holder.getItem(0).copy());
    }

    private void dropOff(ServerLevel level, Trip trip) {
        SimpleContainer holder = new SimpleContainer(miniMe.getFactoryCarry().copy());
        ResourceHandler<ItemResource> carried = VanillaContainerWrapper.of(holder);
        FactoryTransfer.move(carried, FactoryTransfer.at(level, trip.to(), trip.toSide()), resource -> true, 64);

        if (!holder.getItem(0).isEmpty()) {
            for (BlockPos input : factory.storages()) {
                FactoryTransfer.move(carried, FactoryTransfer.at(level, input, null), resource -> true, 64);
                if (holder.getItem(0).isEmpty()) break;
            }
        }
        if (!holder.getItem(0).isEmpty()) {
            Containers.dropItemStack(level, miniMe.getX(), miniMe.getY(), miniMe.getZ(), holder.getItem(0).copy());
            holder.setItem(0, ItemStack.EMPTY);
        }
        miniMe.setFactoryCarry(ItemStack.EMPTY);
    }
}
