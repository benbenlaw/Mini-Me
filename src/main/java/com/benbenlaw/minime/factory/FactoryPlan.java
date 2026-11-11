package com.benbenlaw.minime.factory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public record FactoryPlan(List<Ingredient> ingredients, ItemStack result, String label) {
}
