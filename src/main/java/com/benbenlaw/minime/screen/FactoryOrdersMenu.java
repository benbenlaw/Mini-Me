package com.benbenlaw.minime.screen;

import com.benbenlaw.core.screen.SimpleAbstractContainerMenu;
import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import com.benbenlaw.minime.screen.slot.FactoryPatternSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;

public class FactoryOrdersMenu extends SimpleAbstractContainerMenu {
    public static final int PATTERN_SLOT_INDEX = 36;
    public static final int BUTTON_REMOVE_BASE = 1000;
    public static final int BUTTON_UP_BASE = 2000;
    public static final int BUTTON_DOWN_BASE = 3000;
    public static final int BUTTON_AMOUNT_BASE = 100000;
    public static final int AMOUNT_RANGE = 4096;
    public static final int PATTERN_SLOT_X = 9;
    public static final int PATTERN_SLOT_Y = 36;

    private final MiniMeFactoryBlockEntity blockEntity;
    private final Level level;
    private int status;

    public FactoryOrdersMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public FactoryOrdersMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(MMMenus.FACTORY_ORDERS_MENU.get(), containerId, inventory, pos, 1);
        this.level = inventory.player.level();
        this.blockEntity = (MiniMeFactoryBlockEntity) this.level.getBlockEntity(pos);

        this.addSlot(new FactoryPatternSlot(blockEntity.getPatternHandler(), blockEntity.getPatternHandler()::set,
                0, PATTERN_SLOT_X, PATTERN_SLOT_Y));

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
    }

    public MiniMeFactoryBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public void broadcastChanges() {
        if (!level.isClientSide()) {
            ItemStack dropped = ItemUtil.getStack(blockEntity.getPatternHandler(), 0);
            if (!dropped.isEmpty()) {
                blockEntity.addOrder(dropped);
                blockEntity.getPatternHandler().set(0, ItemResource.EMPTY, 0);
            }
        }
        super.broadcastChanges();
    }

    private void addOrder(ItemStack stack) {
        if (!level.isClientSide()) blockEntity.addOrder(stack);
    }

    public static int amountButtonId(int index, int amount) {
        return BUTTON_AMOUNT_BASE + index * AMOUNT_RANGE + amount;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level.isClientSide()) return false;

        if (id >= BUTTON_AMOUNT_BASE) {
            int encoded = id - BUTTON_AMOUNT_BASE;
            blockEntity.setAmount(encoded / AMOUNT_RANGE, encoded % AMOUNT_RANGE);
            return true;
        }
        if (id >= BUTTON_DOWN_BASE) {
            blockEntity.moveOrder(id - BUTTON_DOWN_BASE, 1);
            return true;
        }
        if (id >= BUTTON_UP_BASE) {
            blockEntity.moveOrder(id - BUTTON_UP_BASE, -1);
            return true;
        }
        if (id >= BUTTON_REMOVE_BASE) {
            blockEntity.removeOrder(id - BUTTON_REMOVE_BASE);
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId == PATTERN_SLOT_INDEX) {
            if (input == ContainerInput.PICKUP || input == ContainerInput.SWAP || input == ContainerInput.CLONE) {
                addOrder(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < PATTERN_SLOT_INDEX) {
            addOrder(this.slots.get(index).getItem());
        }
        return ItemStack.EMPTY;
    }
}
