package com.benbenlaw.minime.factory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.ArrayList;
import java.util.Comparator;
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
        found.sort(Comparator.comparingInt(plan -> plan.kind().ordinal()));
        return found;
    }

    private static FactoryPlan plan(Recipe<?> recipe) {
        if (recipe instanceof AbstractCookingRecipe cooking) {
            FactoryPlan.Kind kind = null;
            if (cooking.getType() == RecipeType.SMELTING) kind = FactoryPlan.Kind.SMELT;
            if (cooking.getType() == RecipeType.BLASTING) kind = FactoryPlan.Kind.BLAST;
            if (cooking.getType() == RecipeType.SMOKING) kind = FactoryPlan.Kind.SMOKE;
            if (kind == null) return null;

            return build(kind, List.of(cooking.input()), cooking.assemble(new SingleRecipeInput(ItemStack.EMPTY)));
        }
        if (recipe instanceof StonecutterRecipe stonecutter) {
            return build(FactoryPlan.Kind.STONECUTTER, List.of(stonecutter.input()), stonecutter.assemble(new SingleRecipeInput(ItemStack.EMPTY)));
        }
        if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
            CraftingRecipe crafting = (CraftingRecipe) recipe;
            List<Ingredient> ingredients = crafting.placementInfo().ingredients();
            if (ingredients.isEmpty()) return null;
            ItemStack result = assembleCrafting(crafting, ingredients);
            if (result == null) return null;
            return build(FactoryPlan.Kind.CRAFT, ingredients, result);
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

    private static FactoryPlan build(FactoryPlan.Kind kind, List<Ingredient> ingredients, ItemStack result) {
        if (result.isEmpty()) return null;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) return null;
        }
        return new FactoryPlan(kind, List.copyOf(ingredients), result, label(kind, ingredients, result));
    }

    private static String label(FactoryPlan.Kind kind, List<Ingredient> ingredients, ItemStack result) {
        String verb = switch (kind) {
            case SMELT -> "Smelting";
            case BLAST -> "Blasting";
            case SMOKE -> "Smoking";
            case CRAFT -> "Crafting";
            case STONECUTTER -> "Stonecutting";
        };

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ingredient : ingredients) {
            String name = ingredient.items().findFirst().map(holder -> new ItemStack(holder).getHoverName().getString()).orElse("?");
            counts.merge(name, 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((name, count) -> parts.add(count > 1 ? count + "x " + name : name));

        return verb + ": " + String.join(", ", parts) + " -> " + result.getCount() + "x " + result.getHoverName().getString();
    }
}
