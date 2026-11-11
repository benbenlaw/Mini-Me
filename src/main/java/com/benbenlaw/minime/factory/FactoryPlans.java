package com.benbenlaw.minime.factory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FactoryPlans {
    private FactoryPlans() {
    }

    public static List<FactoryPlan> find(ServerLevel level, ItemStack target) {
        List<FactoryPlan> found = new ArrayList<>();
        if (target.isEmpty()) return found;

        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            FactoryPlan plan;
            try {
                plan = plan(holder.value());
            } catch (RuntimeException e) {
                continue;
            }
            if (plan != null && ItemStack.isSameItemSameComponents(plan.result(), target)) found.add(plan);
        }
        return found;
    }

    private static FactoryPlan plan(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
            CraftingRecipe crafting = (CraftingRecipe) recipe;
            List<Ingredient> ingredients = crafting.placementInfo().ingredients();
            if (ingredients.isEmpty()) return null;
            ItemStack result = assembleCrafting(crafting, ingredients);
            if (result == null) return null;
            return build(ingredients, result);
        }
        return null;
    }

    private static ItemStack assembleCrafting(CraftingRecipe crafting, List<Ingredient> ingredients) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            stacks.add(ingredient.items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY));
        }

        int width = 3;
        int height = 3;
        if (crafting instanceof ShapedRecipe shaped) {
            width = Math.max(3, shaped.getWidth());
            height = Math.max(3, shaped.getHeight());
        }
        while (stacks.size() < width * height) stacks.add(ItemStack.EMPTY);

        try {
            return crafting.assemble(CraftingInput.of(width, height, stacks));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static FactoryPlan build(List<Ingredient> ingredients, ItemStack result) {
        if (result.isEmpty()) return null;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) return null;
        }
        return new FactoryPlan(List.copyOf(ingredients), result, label(ingredients, result));
    }

    private static String label(List<Ingredient> ingredients, ItemStack result) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ingredient : ingredients) {
            String name = ingredient.items().findFirst().map(holder -> new ItemStack(holder).getHoverName().getString()).orElse("?");
            counts.merge(name, 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((name, count) -> parts.add(count > 1 ? count + "x " + name : name));

        return "Crafting: " + String.join(", ", parts) + " -> " + result.getCount() + "x " + result.getHoverName().getString();
    }
}
