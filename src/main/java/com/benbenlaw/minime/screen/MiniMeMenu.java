package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.screen.slot.MenuSlot;
import com.benbenlaw.minime.screen.slot.MiniMeArmorSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public class MiniMeMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_AREA = 0;
    public static final int BUTTON_TOGGLE_TASK = 100;
    public static final int BUTTON_TASK_UP = 200;
    public static final int BUTTON_TASK_DOWN = 300;
    public static final int BUTTON_QUARRY_WIDTH_DOWN = 400;
    public static final int BUTTON_QUARRY_WIDTH_UP = 401;

    private Container miniMeContainer;
    private boolean slotsActive = true;
    public MiniMeEntity miniMeEntity;

    public static MiniMeMenu create(int i, Inventory inventory, RegistryFriendlyByteBuf registryFriendlyByteBuf) {
        UUID uuid = registryFriendlyByteBuf.readUUID();
        List<MiniMeEntity> miniMeEntityList = inventory.player.level().getEntitiesOfClass(MiniMeEntity.class,
                inventory.player.getBoundingBox().inflate(16), test -> test.getUUID().equals(uuid));
        MiniMeEntity miniMeEntity = miniMeEntityList.isEmpty() ? null : miniMeEntityList.getFirst();
        return new MiniMeMenu(i, inventory, new SimpleContainer(21), miniMeEntity);
    }

    public MiniMeMenu(int containerId, Inventory inventory, Container miniMeContainer, final MiniMeEntity miniMeEntity) {
        super(MMMenus.MINI_ME_MENU.get(), containerId);
        this.miniMeContainer = miniMeContainer;
        this.miniMeEntity = miniMeEntity;
        miniMeContainer.startOpen(inventory.player);

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        this.addSlot(new MiniMeArmorSlot(this, miniMeContainer, MiniMeEntity.HEAD_SLOT, 8, 9, EquipmentSlot.HEAD));
        this.addSlot(new MiniMeArmorSlot(this, miniMeContainer, MiniMeEntity.CHEST_SLOT, 8, 27, EquipmentSlot.CHEST));
        this.addSlot(new MiniMeArmorSlot(this, miniMeContainer, MiniMeEntity.LEGS_SLOT, 8, 45, EquipmentSlot.LEGS));
        this.addSlot(new MiniMeArmorSlot(this, miniMeContainer, MiniMeEntity.BOOTS_SLOT,8, 63, EquipmentSlot.FEET));

        this.addSlot(new MenuSlot(this, miniMeContainer, MiniMeEntity.MAIN_HAND, 26, 63));
        this.addSlot(new MenuSlot(this, miniMeContainer, MiniMeEntity.OFF_HAND, 44, 63));

        int startX = 72;
        int startY = 27;
        int columns = 5;
        int rows = 3;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotIndex = 6 + row * columns + col;
                int xPos = startX + col * 18;
                int yPos = startY + row * 18;
                this.addSlot(new MenuSlot(this, miniMeContainer, slotIndex, xPos, yPos));
            }
        }
    }


    public boolean areSlotsActive() {
        return slotsActive;
    }

    public void setSlotsActive(boolean active) {
        this.slotsActive = active;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.miniMeEntity == null) return false;

        if (id == BUTTON_TOGGLE_AREA) {
            this.miniMeEntity.setShowingArea(!this.miniMeEntity.isShowingArea());
            return true;
        }
        if (id == BUTTON_QUARRY_WIDTH_DOWN || id == BUTTON_QUARRY_WIDTH_UP) {
            this.miniMeEntity.changeQuarryWidth(id == BUTTON_QUARRY_WIDTH_UP ? 1 : -1);
            return true;
        }
        if (id >= BUTTON_TASK_DOWN) {
            this.miniMeEntity.moveTask(id - BUTTON_TASK_DOWN, 1);
            return true;
        }
        if (id >= BUTTON_TASK_UP) {
            this.miniMeEntity.moveTask(id - BUTTON_TASK_UP, -1);
            return true;
        }
        if (id >= BUTTON_TOGGLE_TASK) {
            this.miniMeEntity.toggleTask(id - BUTTON_TOGGLE_TASK);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.miniMeEntity != null
                && this.miniMeContainer.stillValid(player)
                && this.miniMeEntity.isAlive()
                && player.isWithinEntityInteractionRange(this.miniMeEntity, 4.0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            int i = this.miniMeContainer.getContainerSize() + 1;
            if (index < i) {
                if (!this.moveItemStackTo(itemstack1, i, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).mayPlace(itemstack1)) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (i <= 1 || !this.moveItemStackTo(itemstack1, 2, i, false)) {
                int j = i + 27;
                int k = j + 9;
                if (index >= j && index < k) {
                    if (!this.moveItemStackTo(itemstack1, i, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= i && index < j) {
                    if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.miniMeContainer.stopOpen(player);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new MenuSlot(this, playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new MenuSlot(this, playerInventory, i, 8 + i * 18, 142));
        }
    }
}