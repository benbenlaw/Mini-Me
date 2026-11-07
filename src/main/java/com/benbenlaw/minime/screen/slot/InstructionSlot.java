package com.benbenlaw.minime.screen.slot;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.item.MMItems;
import com.benbenlaw.minime.screen.MiniMeMenu;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class InstructionSlot extends MenuSlot {
    public InstructionSlot(MiniMeMenu menu, Container container, int index, int x, int y) {
        super(menu, container, index, x, y);
        this.setBackground(MiniMe.identifier( "container/slot/instructions"));
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(MMItems.INSTRUCTIONS.get());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
