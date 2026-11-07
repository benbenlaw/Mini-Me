package com.benbenlaw.minime.block.entity;

import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.item.SyncableItemHandler;
import com.benbenlaw.minime.block.MiniMeFactoryBlock;
import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.factory.FactoryPlan;
import com.benbenlaw.minime.factory.FactoryPlans;
import com.benbenlaw.minime.factory.FactoryRegistry;
import com.benbenlaw.minime.factory.FactoryStatus;
import com.benbenlaw.minime.screen.MiniMeFactoryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MiniMeFactoryBlockEntity extends SyncableBlockEntity implements MenuProvider {
    private static final long WORKER_TIMEOUT = 40;
    private static final long STATUS_TIMEOUT = 60;

    public static final int MAX_ORDERS = 24;
    public static final int MAX_STOCK = 2048;
    public static final int DEFAULT_STOCK = 16;

    public record Order(ItemStack item, int amount) {
    }

    private final SyncableItemHandler pattern = new SyncableItemHandler(this, 1, (slot, stack) -> true, slot -> true);

    private int status = FactoryStatus.IDLE.ordinal();
    private long statusTick = Long.MIN_VALUE / 2;
    private UUID worker;
    private long workerTick;
    private String activeLabel = "";
    private boolean showingArea;

    private final List<Order> orders = new ArrayList<>();
    private List<Integer> stocks = new ArrayList<>();
    private final Map<Integer, List<FactoryPlan>> planCache = new HashMap<>();

    public MiniMeFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(MMBlockEntities.MINI_ME_FACTORY.get(), pos, state);
    }

    public ItemStacksResourceHandler getPatternHandler() {
        return pattern;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public int stockOf(int index) {
        return index >= 0 && index < stocks.size() ? stocks.get(index) : 0;
    }

    public String getActiveLabel() {
        return activeLabel;
    }

    private void changed() {
        setChanged();
        sync();
    }

    public boolean addOrder(ItemStack stack) {
        if (stack.isEmpty() || orders.size() >= MAX_ORDERS) return false;
        for (Order order : orders) {
            if (ItemStack.isSameItemSameComponents(order.item(), stack)) return false;
        }
        orders.add(new Order(stack.copyWithCount(1), DEFAULT_STOCK));
        changed();
        return true;
    }

    public void removeOrder(int index) {
        if (index < 0 || index >= orders.size()) return;
        orders.remove(index);
        if (index < stocks.size()) stocks.remove(index);
        changed();
    }

    public void moveOrder(int index, int direction) {
        int target = index + direction;
        if (index < 0 || index >= orders.size() || target < 0 || target >= orders.size()) return;

        java.util.Collections.swap(orders, index, target);
        if (index < stocks.size() && target < stocks.size()) java.util.Collections.swap(stocks, index, target);
        changed();
    }

    public void setAmount(int index, int amount) {
        if (index < 0 || index >= orders.size()) return;
        int clamped = Math.max(1, Math.min(MAX_STOCK, amount));
        if (orders.get(index).amount() == clamped) return;
        orders.set(index, new Order(orders.get(index).item(), clamped));
        changed();
    }

    public void setStocks(List<Integer> values) {
        if (stocks.equals(values)) return;
        stocks = new ArrayList<>(values);
        changed();
    }

    public void setActive(String label) {
        if (activeLabel.equals(label)) return;
        activeLabel = label;
        changed();
    }

    public int radius() {
        return MiniMeConfig.FACTORY_RADIUS.get();
    }

    public BlockPos areaStart() {
        Direction back = getBlockState().getValue(MiniMeFactoryBlock.FACING);
        return worldPosition.relative(back, 1).relative(back.getClockWise(), -radius());
    }

    public BlockPos areaEnd() {
        Direction back = getBlockState().getValue(MiniMeFactoryBlock.FACING);
        int radius = radius();
        return worldPosition.relative(back, 2 * radius + 1).relative(back.getClockWise(), radius).above(2 * radius);
    }

    public AABB getArea() {
        BlockPos a = areaStart();
        BlockPos b = areaEnd();
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    public boolean inArea(BlockPos pos) {
        if (pos.equals(this.worldPosition)) return false;

        AABB area = getArea();
        return pos.getX() >= area.minX && pos.getX() < area.maxX
                && pos.getY() >= area.minY && pos.getY() < area.maxY
                && pos.getZ() >= area.minZ && pos.getZ() < area.maxZ;
    }

    public boolean isShowingArea() {
        return showingArea;
    }

    public void toggleShowingArea() {
        showingArea = !showingArea;
        setChanged();
        sync();
    }

    public List<BlockPos> containers() {
        List<BlockPos> found = new ArrayList<>();
        if (level == null) return found;

        for (BlockPos pos : BlockPos.betweenClosed(areaStart(), areaEnd())) {
            if (pos.equals(worldPosition)) continue;
            if (level.getCapability(Capabilities.Item.BLOCK, pos, null) != null) found.add(pos.immutable());
        }
        return found;
    }

    public List<BlockPos> storages() {
        List<BlockPos> found = new ArrayList<>();
        if (level == null) return found;

        for (BlockPos pos : containers()) {
            if (!(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity)
                    || level.getBlockEntity(pos) instanceof HopperBlockEntity) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                    && found.contains(pos.relative(ChestBlock.getConnectedDirection(state)))) {
                continue;
            }
            found.add(pos);
        }
        return found;
    }

    public List<BlockPos> machines() {
        List<BlockPos> found = new ArrayList<>();
        if (level == null) return found;

        for (BlockPos pos : containers()) {
            if (!(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity)) found.add(pos);
        }
        return found;
    }

    public List<FactoryPlan> plans(ServerLevel serverLevel, ItemStack target) {
        return planCache.computeIfAbsent(ItemStack.hashItemAndComponents(target), key -> FactoryPlans.find(serverLevel, target));
    }

    public boolean tryClaim(UUID id, long gameTime) {
        if (worker == null || worker.equals(id) || gameTime - workerTick > WORKER_TIMEOUT) {
            worker = id;
            workerTick = gameTime;
            return true;
        }
        return false;
    }

    public void release(UUID id) {
        if (id.equals(worker)) worker = null;
    }

    public void setStatus(FactoryStatus value) {
        status = value.ordinal();
        if (level != null) statusTick = level.getGameTime();
    }

    public int getStatusCode() {
        if (level != null && level.getGameTime() - statusTick > STATUS_TIMEOUT) return FactoryStatus.NO_WORKER.ordinal();
        return status;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        output.putInt("orderCount", orders.size());
        for (int i = 0; i < orders.size(); i++) {
            output.store("orderItem" + i, ItemStack.OPTIONAL_CODEC, orders.get(i).item());
            output.putInt("orderAmount" + i, orders.get(i).amount());
            output.putInt("orderStock" + i, stockOf(i));
        }
        output.putString("activeLabel", activeLabel);
        output.putBoolean("showArea", showingArea);
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        orders.clear();
        List<Integer> loadedStocks = new ArrayList<>();
        int count = input.getIntOr("orderCount", 0);
        for (int i = 0; i < count; i++) {
            ItemStack item = input.read("orderItem" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            if (item.isEmpty()) continue;
            orders.add(new Order(item, Math.clamp(input.getIntOr("orderAmount" + i, DEFAULT_STOCK), 1, MAX_STOCK)));
            loadedStocks.add(input.getIntOr("orderStock" + i, 0));
        }
        stocks = loadedStocks;
        activeLabel = input.getStringOr("activeLabel", "");
        showingArea = input.getBooleanOr("showArea", false);
        super.loadAdditional(input);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) FactoryRegistry.add(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) FactoryRegistry.remove(level, worldPosition);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int container, @NonNull Inventory inventory, @NonNull Player player) {
        return new MiniMeFactoryMenu(container, inventory, this.worldPosition);
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("block.minime.mini_me_factory");
    }
}
