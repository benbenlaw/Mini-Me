package com.benbenlaw.minime.screen;

import com.benbenlaw.core.screen.SimpleAbstractContainerMenu;
import com.benbenlaw.core.screen.util.slot.InputSlot;
import com.benbenlaw.minime.block.entity.InstructionDeskBlockEntity;
import com.benbenlaw.minime.datamaps.InstructionCosts;
import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeTask;
import com.benbenlaw.minime.item.MMDataComponents;
import com.benbenlaw.minime.item.MMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class InstructionDeskMenu extends SimpleAbstractContainerMenu {
    public static final int INSTRUCTION_SLOT_X = 8;
    public static final int INSTRUCTION_SLOT_Y = 35;
    public static final int BUTTON_REMOVE_BASE = 1000;

    private final InstructionDeskBlockEntity blockEntity;
    private final Level level;

    public InstructionDeskMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public InstructionDeskMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(MMMenus.INSTRUCTION_DESK_MENU.get(), containerId, inventory, pos, 1);
        this.level = inventory.player.level();
        this.blockEntity = (InstructionDeskBlockEntity) this.level.getBlockEntity(pos);

        this.addSlot(new InputSlot(blockEntity.getItemHandler(), blockEntity.getItemHandler()::set, 0, INSTRUCTION_SLOT_X, INSTRUCTION_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(MMItems.INSTRUCTIONS.get());
            }
        }.size(1));
    }

    public ItemStack getInstructionStack() {
        return this.slots.get(36).getItem();
    }

    public static List<MiniMeTask> merge(List<MiniMeTask> current, List<MiniMeTask> added) {
        List<MiniMeTask> merged = new ArrayList<>(current);
        for (MiniMeTask task : added) {
            if (!merged.contains(task)) merged.add(task);
        }
        return merged;
    }

    public static List<MiniMeTask> without(List<MiniMeTask> current, List<MiniMeTask> removed) {
        List<MiniMeTask> remaining = new ArrayList<>(current);
        remaining.removeAll(removed);
        return remaining;
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int id) {
        if (level.isClientSide()) return false;

        boolean remove = id >= BUTTON_REMOVE_BASE;
        int index = remove ? id - BUTTON_REMOVE_BASE : id;

        List<InstructionCosts.Entry> entries = InstructionCosts.all();
        if (index < 0 || index >= entries.size()) return false;

        ItemStack instructions = getInstructionStack();
        if (!instructions.is(MMItems.INSTRUCTIONS.get())) return false;

        InstructionCosts.Entry entry = entries.get(index);
        List<MiniMeTask> current = MiniMeEntity.tasksOf(instructions);
        List<MiniMeTask> result = remove ? without(current, entry.tasks()) : merge(current, entry.tasks());
        if (result.size() == current.size()) return false;

        if (!remove && !player.hasInfiniteMaterials() && !takeOne(player, entry.items())) return false;

        ItemStack updated = instructions.copy();
        if (result.isEmpty()) {
            updated.remove(MMDataComponents.INSTRUCTION_TASKS.get());
        } else {
            updated.set(MMDataComponents.INSTRUCTION_TASKS.get(), result);
        }

        blockEntity.getItemHandler().set(0, ItemResource.of(updated), 1);
        blockEntity.setChanged();
        this.broadcastChanges();
        return true;
    }

    public static boolean hasItem(Player player, List<Item> items) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (items.contains(inventory.getItem(i).getItem())) return true;
        }
        return false;
    }

    public static Item firstOwned(Player player, List<Item> items) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            Item item = inventory.getItem(i).getItem();
            if (items.contains(item)) return item;
        }
        return null;
    }

    private static boolean takeOne(Player player, List<Item> items) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && items.contains(stack.getItem())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
