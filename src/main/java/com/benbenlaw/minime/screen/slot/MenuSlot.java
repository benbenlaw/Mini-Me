package com.benbenlaw.minime.screen.slot;

import com.benbenlaw.minime.screen.MiniMeMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class MenuSlot extends Slot {

    private final MiniMeMenu menu;

    public MenuSlot(MiniMeMenu menu, Container container, int index, int x, int y) {
        super(container, index, x, y);
        this.menu = menu;
    }

    @Override
    public boolean isActive() {
        return menu.areSlotsActive();
    }
}
