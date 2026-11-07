package com.benbenlaw.minime.data;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.MMBlocks;
import com.benbenlaw.minime.item.MMItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class MMRecipeProvider extends RecipeProvider {
    public MMRecipeProvider(HolderLookup.Provider provider, RecipeOutput output) {
        super(provider, output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
            super(packOutput, provider);
        }

        @Override
        protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider provider, @NotNull RecipeOutput recipeOutput) {
            return new MMRecipeProvider(provider, recipeOutput);
        }

        @Override
        public @NotNull String getName() {
            return MiniMe.MOD_ID + " Recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        shaped(RecipeCategory.MISC, MMBlocks.INSTRUCTION_DESK)
                .pattern("PBP")
                .pattern("S S")
                .pattern("S S")
                .define('P', ItemTags.PLANKS)
                .define('B', Items.BOOK)
                .define('S', Items.STICK)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(output);

        shaped(RecipeCategory.MISC, MMBlocks.MINI_ME_CONSTRUCTOR)
                .pattern("IHI")
                .pattern("RGR")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('H', Items.PLAYER_HEAD)
                .define('R', Items.REDSTONE)
                .define('G', Items.GOLD_BLOCK)
                .unlockedBy("has_gold_block", has(Items.GOLD_BLOCK))
                .save(output);

        shaped(RecipeCategory.MISC, MMBlocks.MINI_ME_FACTORY)
                .pattern("IHI")
                .pattern("PCP")
                .pattern("IPI")
                .define('I', Items.IRON_INGOT)
                .define('H', Items.HOPPER)
                .define('P', ItemTags.PLANKS)
                .define('C', Items.CRAFTING_TABLE)
                .unlockedBy("has_crafting_table", has(Items.CRAFTING_TABLE))
                .save(output);

        shaped(RecipeCategory.TOOLS, MMItems.COMMAND_TOOL)
                .pattern(" G")
                .pattern("S ")
                .define('G', Items.GOLD_NUGGET)
                .define('S', Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK))
                .save(output);

        shapeless(RecipeCategory.MISC, MMItems.INSTRUCTIONS)
                .requires(Items.PAPER)
                .requires(Items.GOLD_NUGGET)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);
    }
}
