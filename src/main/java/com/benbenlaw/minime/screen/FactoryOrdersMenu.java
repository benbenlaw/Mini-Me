package com.benbenlaw.minime.screen;

import com.benbenlaw.core.screen.SimpleAbstractContainerMenu;
import com.benbenlaw.minime.block.entity.MiniMeFactoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FactoryOrdersMenu extends SimpleAbstractContainerMenu {
    public static final int BUTTON_REMOVE_BASE = 1000;
    public static final int BUTTON_MOVE_BASE = 4000;
    public static final int BUTTON_ADD_CARRIED = 5000;
    public static final int BUTTON_AMOUNT_BASE = 100000;
    public static final int AMOUNT_RANGE = 4096;
    public static final int MOVE_RANGE = 32;

    private final MiniMeFactoryBlockEntity blockEntity;
    private final Level level;
    private int status;

    public FactoryOrdersMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public FactoryOrdersMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(MMMenus.FACTORY_ORDERS_MENU.get(), containerId, inventory, pos, 0);
        this.level = inventory.player.level();
        this.blockEntity = (MiniMeFactoryBlockEntity) this.level.getBlockEntity(pos);

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

    public static int amountButtonId(int index, int amount) {
        return BUTTON_AMOUNT_BASE + index * AMOUNT_RANGE + amount;
    }

    public static int moveButtonId(int from, int to) {
        return BUTTON_MOVE_BASE + from * MOVE_RANGE + to;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
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
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!level.isClientSide() && index >= 0 && index < this.slots.size()) {
            blockEntity.addOrder(this.slots.get(index).getItem());
        }
        return ItemStack.EMPTY;
    }
}
