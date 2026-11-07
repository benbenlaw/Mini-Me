package com.benbenlaw.minime.block.entity;

import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.item.SyncableItemHandler;
import com.benbenlaw.minime.item.MMItems;
import com.benbenlaw.minime.screen.InstructionDeskMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class InstructionDeskBlockEntity extends SyncableBlockEntity implements MenuProvider {
    private final SyncableItemHandler inventory = new SyncableItemHandler(this, 1,
            (slot, stack) -> stack.is(MMItems.INSTRUCTIONS.get()), slot -> true);

    public InstructionDeskBlockEntity(BlockPos pos, BlockState state) {
        super(MMBlockEntities.INSTRUCTION_DESK.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        inventory.serialize(output.child("inventory"));
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        inventory.deserialize(input.childOrEmpty("inventory"));
        super.loadAdditional(input);
    }

    public ItemStacksResourceHandler getItemHandler() {
        return inventory;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int container, @NonNull Inventory inventory, @NonNull Player player) {
        return new InstructionDeskMenu(container, inventory, this.worldPosition);
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("block.minime.instruction_desk");
    }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos pos, @NonNull BlockState state) {
        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, new SimpleContainer(ItemUtil.getStack(inventory, 0)));
    }
}
