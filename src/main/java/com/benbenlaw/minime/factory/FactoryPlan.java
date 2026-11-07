package com.benbenlaw.minime.factory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public record FactoryPlan(Kind kind, List<Ingredient> ingredients, ItemStack result, String label) {
    public enum Kind {
        SMELT, BLAST, SMOKE, CRAFT, STONECUTTER;

        public boolean cooking() {
            return this == SMELT || this == BLAST || this == SMOKE;
        }
    }
}
