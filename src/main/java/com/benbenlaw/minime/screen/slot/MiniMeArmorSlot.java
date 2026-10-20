package com.benbenlaw.minime.screen.slot;

import com.benbenlaw.minime.screen.MiniMeMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.ItemStack;

public class MiniMeArmorSlot extends MenuSlot {

    private final EquipmentSlot equipmentSlot;

    public MiniMeArmorSlot(MiniMeMenu menu, Container container, int index, int x, int y, EquipmentSlot slot) {
        super(menu, container, index, x, y);
        this.equipmentSlot = slot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == equipmentSlot;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
