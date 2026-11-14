package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.block.MMBlocks;
import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import com.benbenlaw.minime.factory.FactoryRecipeFill;
import com.benbenlaw.minime.factory.FactoryStorage;
import com.benbenlaw.minime.factory.ViewEntry;
import com.benbenlaw.minime.network.packet.FactoryViewPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MiniMeFactoryMenu extends AbstractCraftingMenu {
    public static final int BUTTON_TOGGLE_AREA = 0;
    public static final int BUTTON_REMOVE_BASE = 1000;
    public static final int BUTTON_MOVE_BASE = 2000;
    public static final int BUTTON_ADD_CARRIED = 4000;
    public static final int BUTTON_AMOUNT_BASE = 100000;
    public static final int AMOUNT_RANGE = 4096;
    public static final int MOVE_RANGE = 32;
    public static final int RESULT_X = 124;
    public static final int RESULT_Y = 114;
    public static final int GRID_X = 30;
    public static final int GRID_Y = 96;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 163;

    private static final int GRID_START = 1;
    private static final int GRID_END = 10;
    private static final int INVENTORY_START = 10;
    private static final int INVENTORY_END = 46;
    private static final int SYNC_INTERVAL = 5;

    private final MiniMeFactoryBlockEntity blockEntity;
    private final Player player;
    private final Level level;
    private final ContainerLevelAccess access;
    private boolean placingRecipe;
    private boolean refreshResult;
    private int status;

    private Map<ItemResource, Long> lastSent;
    private int syncTimer;
    private boolean forceSync;
    private List<ViewEntry> view = List.of();

    public MiniMeFactoryMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public MiniMeFactoryMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(MMMenus.FACTORY_MENU.get(), containerId, 3, 3);
        this.player = inventory.player;
        this.level = inventory.player.level();
        this.blockEntity = (MiniMeFactoryBlockEntity) this.level.getBlockEntity(pos);
        this.access = ContainerLevelAccess.create(this.level, pos);

        this.addResultSlot(this.player, RESULT_X, RESULT_Y);
        this.addCraftingGridSlots(GRID_X, GRID_Y);
        this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return level.isClientSide() ? status : blockEntity.getStatusCode();
            }

            @Override
            public void set(int value) {
                status = value;
            }
        });

        if (!level.isClientSide()) {
            placingRecipe = true;
            for (int i = 0; i < 9; i++) craftSlots.setItem(i, blockEntity.getGridItem(i).copy());
            placingRecipe = false;
            refreshResult = true;
        }
    }

    public MiniMeFactoryBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public List<ViewEntry> getView() {
        return view;
    }

    public void setView(List<ViewEntry> entries) {
        this.view = entries;
    }

    private ItemStack[] snapshotGrid() {
        ItemStack[] before = new ItemStack[9];
        for (int i = 0; i < 9; i++) before[i] = craftSlots.getItem(i).copy();
        return before;
    }

    private void refill(ItemStack[] before) {
        if (level.isClientSide()) return;

        for (int i = 0; i < 9; i++) {
            if (!craftSlots.getItem(i).isEmpty() || before[i].isEmpty()) continue;

            ItemStack pulled = FactoryStorage.extract(level, blockEntity, before[i], 1);
            if (!pulled.isEmpty()) craftSlots.setItem(i, pulled);
        }
        forceSync = true;
    }

    public void fillFromStorage(ServerPlayer filler, List<FactoryRecipeFill.Cell> cells, boolean max) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftSlots.removeItemNoUpdate(i);
            if (stack.isEmpty()) continue;

            int left = FactoryStorage.insert(level, blockEntity, stack);
            if (left > 0) {
                ItemStack rest = stack.copyWithCount(left);
                if (!filler.getInventory().add(rest)) filler.drop(rest, false);
            }
        }

        Map<ItemResource, Long> totals = FactoryStorage.aggregate(level, blockEntity);
        FactoryRecipeFill.Result result = FactoryRecipeFill.plan(cells, stack -> totals.getOrDefault(ItemResource.of(stack), 0L), max);
        if (result.missing().isEmpty()) {
            result.picks().forEach((slot, key) -> {
                if (slot < 0 || slot >= 9) return;
                ItemStack taken = FactoryStorage.extract(level, blockEntity, key, result.sets());
                if (!taken.isEmpty()) craftSlots.setItem(slot, taken);
            });
        }

        slotsChanged(craftSlots);
        forceSync = true;
    }

    public void handleViewClick(ServerPlayer clicker, ItemStack key, int button, boolean shift) {
        ItemStack carried = getCarried();

        if (!carried.isEmpty()) {
            int count = button == 1 ? 1 : carried.getCount();
            ItemStack part = carried.copyWithCount(count);
            int left = FactoryStorage.insert(level, blockEntity, part);
            carried.shrink(count - left);
            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            forceSync = true;
            return;
        }
        if (key.isEmpty()) return;

        int max = key.getMaxStackSize();
        int want = button == 1 ? Math.max(1, max / 2) : max;
        ItemStack taken = FactoryStorage.extract(level, blockEntity, key, want);
        if (taken.isEmpty()) return;

        if (shift) {
            clicker.getInventory().add(taken);
            if (!taken.isEmpty()) {
                int left = FactoryStorage.insert(level, blockEntity, taken);
                if (left > 0) clicker.drop(taken.copyWithCount(left), false);
            }
        } else {
            setCarried(taken);
        }
        forceSync = true;
    }

    @Override
    public void broadcastChanges() {
        if (!level.isClientSide()) {
            if (refreshResult) {
                refreshResult = false;
                slotsChanged(craftSlots);
            }
            for (int i = 0; i < 9; i++) blockEntity.setGridItem(i, craftSlots.getItem(i));
        }
        super.broadcastChanges();
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return;

        if (!forceSync && --syncTimer > 0) return;
        syncTimer = SYNC_INTERVAL;
        forceSync = false;

        Map<ItemResource, Long> now = FactoryStorage.aggregate(level, blockEntity);
        if (now.equals(lastSent)) return;

        lastSent = now;
        PacketDistributor.sendToPlayer(serverPlayer, new FactoryViewPacket(this.containerId, FactoryStorage.sorted(now)));
    }

    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, ServerLevel level, Player player,
                                                  CraftingContainer container, ResultContainer resultSlots,
                                                  RecipeHolder<CraftingRecipe> recipeHint) {
        CraftingInput input = container.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> maybeRecipe = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level, recipeHint);
        if (maybeRecipe.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = maybeRecipe.get();
            if (resultSlots.setRecipeUsed(serverPlayer, holder)) {
                ItemStack assembled = holder.value().assemble(input);
                if (assembled.isItemEnabled(level.enabledFeatures())) result = assembled;
            }
        }

        resultSlots.setItem(0, result);
        menu.setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, result));
    }

    @Override
    public void slotsChanged(Container container) {
        if (!placingRecipe && level instanceof ServerLevel serverLevel) {
            slotChangedCraftingGrid(this, serverLevel, player, craftSlots, resultSlots, null);
        }
    }

    @Override
    public void beginPlacingRecipe() {
        this.placingRecipe = true;
    }

    @Override
    public void finishPlacingRecipe(ServerLevel serverLevel, RecipeHolder<CraftingRecipe> recipe) {
        this.placingRecipe = false;
        slotChangedCraftingGrid(this, serverLevel, player, craftSlots, resultSlots, recipe);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (slotId == 0 && input == ContainerInput.QUICK_MOVE) {
            craftOneStack(clicker);
            return;
        }

        ItemStack[] before = slotId == 0 ? snapshotGrid() : null;
        super.clicked(slotId, button, input, clicker);
        if (before != null) refill(before);
    }

    private void craftOneStack(Player clicker) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        Slot slot = slots.get(0);
        ItemStack first = slot.getItem().copy();
        if (first.isEmpty()) return;

        int limit = first.getMaxStackSize();
        int made = 0;
        while (made < limit) {
            ItemStack result = slot.getItem();
            if (result.isEmpty() || !ItemStack.isSameItemSameComponents(result, first) || made + result.getCount() > limit) break;

            ItemStack[] before = snapshotGrid();
            ItemStack crafted = result.copy();
            slot.onQuickCraft(ItemStack.EMPTY, crafted);
            slot.onTake(clicker, crafted);
            refill(before);
            slotChangedCraftingGrid(this, serverLevel, player, craftSlots, resultSlots, null);

            made += crafted.getCount();
            clicker.getInventory().add(crafted);
            if (!crafted.isEmpty()) clicker.drop(crafted, false);
        }
        forceSync = true;
    }

    @Override
    public void removed(Player leaving) {
        super.removed(leaving);
        if (level.isClientSide()) return;

        for (int i = 0; i < 9; i++) blockEntity.setGridItem(i, craftSlots.getItem(i));
    }

    @Override
    public boolean clickMenuButton(Player user, int id) {
        if (level.isClientSide()) return false;

        if (id >= BUTTON_AMOUNT_BASE) {
            int encoded = id - BUTTON_AMOUNT_BASE;
            blockEntity.setAmount(encoded / AMOUNT_RANGE, encoded % AMOUNT_RANGE);
            return true;
        }
        if (id == BUTTON_ADD_CARRIED) {
            blockEntity.addOrder(getCarried());
            return true;
        }
        if (id >= BUTTON_MOVE_BASE) {
            int encoded = id - BUTTON_MOVE_BASE;
            blockEntity.moveOrderTo(encoded / MOVE_RANGE, encoded % MOVE_RANGE);
            return true;
        }
        if (id >= BUTTON_REMOVE_BASE) {
            blockEntity.removeOrder(id - BUTTON_REMOVE_BASE);
            return true;
        }
        if (id == BUTTON_TOGGLE_AREA) {
            blockEntity.toggleShowingArea();
            return true;
        }
        return false;
    }

    public static int amountButtonId(int index, int amount) {
        return BUTTON_AMOUNT_BASE + index * AMOUNT_RANGE + amount;
    }

    public static int moveButtonId(int from, int to) {
        return BUTTON_MOVE_BASE + from * MOVE_RANGE + to;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public boolean stillValid(Player user) {
        return stillValid(access, user, MMBlocks.MINI_ME_FACTORY.get());
    }

    @Override
    public ItemStack quickMoveStack(Player user, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        ItemStack[] before = slotIndex == 0 ? snapshotGrid() : null;

        if (slotIndex == 0) {
            stack.getItem().onCraftedBy(stack, user);
            if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (slotIndex >= INVENTORY_START) {
            if (!level.isClientSide()) {
                int left = FactoryStorage.insert(level, blockEntity, stack);
                stack.setCount(left);
                forceSync = true;
            } else {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= GRID_START && slotIndex < GRID_END) {
            if (level.isClientSide()) return ItemStack.EMPTY;

            int left = FactoryStorage.insert(level, blockEntity, stack);
            stack.setCount(left);
            forceSync = true;
        } else if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;

        slot.onTake(user, stack);
        if (slotIndex == 0) {
            user.drop(stack, false);
            refill(before);
        }
        return original;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public Slot getResultSlot() {
        return this.slots.get(0);
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return this.slots.subList(GRID_START, GRID_END);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    protected Player owner() {
        return this.player;
    }
}
