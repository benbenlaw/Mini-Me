package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.entity.MiniMeEntity;
import com.benbenlaw.minime.entity.MiniMeSetting;
import com.benbenlaw.minime.item.MMItems;
import com.benbenlaw.minime.network.packet.CustomizePacket;
import com.benbenlaw.minime.screen.slot.InstructionSlot;
import com.benbenlaw.minime.screen.slot.MenuSlot;
import com.benbenlaw.minime.screen.slot.MiniMeArmorSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public class MiniMeMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_AREA = 0;
    public static final int BUTTON_TOGGLE_NAME_TAG = 1;
    public static final int BUTTON_TOGGLE_TASK = 100;
    public static final int BUTTON_TASK_UP = 200;
    public static final int BUTTON_TASK_DOWN = 300;

    public static final int BUTTON_SET_SETTING_BASE = 10000;
    private static final int SETTING_RANGE = 8192;
    private static final int SETTING_OFFSET = 4096;

    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int FIRST_MINI_ME_SLOT = PLAYER_SLOT_COUNT;
    public static final int LAST_ARMOR_SLOT = FIRST_MINI_ME_SLOT + 4;
    public static final int FIRST_STORAGE_MENU_SLOT = LAST_ARMOR_SLOT + 2;
    public static final int INSTRUCTION_MENU_SLOT = FIRST_STORAGE_MENU_SLOT + MiniMeEntity.STORAGE_SLOT_COUNT;

    public static final int INSTRUCTION_SLOT_X = 152;
    public static final int INSTRUCTION_SLOT_Y = 8;

    private static final Identifier EMPTY_TOOL_SLOT = Identifier.withDefaultNamespace("container/slot/pickaxe");

    public static int settingValueButtonId(MiniMeSetting setting, int value) {
        return BUTTON_SET_SETTING_BASE + setting.ordinal() * SETTING_RANGE + (value + SETTING_OFFSET);
    }

    private Container miniMeContainer;
    private boolean slotsActive = true;
    public MiniMeEntity miniMeEntity;

    public static MiniMeMenu create(int i, Inventory inventory, RegistryFriendlyByteBuf registryFriendlyByteBuf) {
        UUID uuid = registryFriendlyByteBuf.readUUID();
        List<MiniMeEntity> miniMeEntityList = inventory.player.level().getEntitiesOfClass(MiniMeEntity.class,
                inventory.player.getBoundingBox().inflate(16), test -> test.getUUID().equals(uuid));
        MiniMeEntity miniMeEntity = miniMeEntityList.isEmpty() ? null : miniMeEntityList.getFirst();
        return new MiniMeMenu(i, inventory, new SimpleContainer(MiniMeEntity.CONTAINER_SIZE), miniMeEntity);
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
        this.addSlot(new MiniMeArmorSlot(this, miniMeContainer, MiniMeEntity.BOOTS_SLOT, 8, 63, EquipmentSlot.FEET));

        this.addSlot(new MenuSlot(this, miniMeContainer, MiniMeEntity.MAIN_HAND, 26, 63).setBackground(EMPTY_TOOL_SLOT));
        this.addSlot(new MenuSlot(this, miniMeContainer, MiniMeEntity.OFF_HAND, 44, 63).setBackground(InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));

        int startX = 72;
        int startY = 27;
        int columns = 5;
        int rows = 3;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotIndex = MiniMeEntity.FIRST_STORAGE_SLOT + row * columns + col;
                int xPos = startX + col * 18;
                int yPos = startY + row * 18;
                this.addSlot(new MenuSlot(this, miniMeContainer, slotIndex, xPos, yPos));
            }
        }

        this.addSlot(new InstructionSlot(this, miniMeContainer, MiniMeEntity.INSTRUCTION_SLOT, INSTRUCTION_SLOT_X, INSTRUCTION_SLOT_Y));
    }

    public void customize(Player player, int kind, String value) {
        if (this.miniMeEntity == null || !stillValid(player)) return;

        String text = value.trim();
        if (kind == CustomizePacket.NAME) {
            this.miniMeEntity.rename(text);
        } else if (kind == CustomizePacket.SKIN && text.matches("[A-Za-z0-9_]{0," + MiniMeEntity.MAX_SKIN_NAME_LENGTH + "}")) {
            this.miniMeEntity.setSkin(text);
        }
    }

    public ItemStack getInstructionStack() {
        return this.slots.get(INSTRUCTION_MENU_SLOT).getItem();
    }

    public boolean hasInstructions() {
        return getInstructionStack().is(MMItems.INSTRUCTIONS.get());
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
        if (id == BUTTON_TOGGLE_NAME_TAG) {
            this.miniMeEntity.setCustomNameVisible(!this.miniMeEntity.isCustomNameVisible());
            return true;
        }
        if (id >= BUTTON_SET_SETTING_BASE) {
            int encoded = id - BUTTON_SET_SETTING_BASE;
            int index = encoded / SETTING_RANGE;
            MiniMeSetting[] settings = MiniMeSetting.values();
            if (index >= settings.length) return false;

            this.miniMeEntity.setSettingValue(settings[index], encoded % SETTING_RANGE - SETTING_OFFSET);
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
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < PLAYER_SLOT_COUNT) {
            boolean moved = this.moveItemStackTo(stack, FIRST_MINI_ME_SLOT, LAST_ARMOR_SLOT, false)
                    || this.moveItemStackTo(stack, INSTRUCTION_MENU_SLOT, INSTRUCTION_MENU_SLOT + 1, false)
                    || this.moveItemStackTo(stack, FIRST_STORAGE_MENU_SLOT, INSTRUCTION_MENU_SLOT, false);
            if (!moved) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, 0, PLAYER_SLOT_COUNT, true)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.miniMeContainer.stopOpen(player);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
