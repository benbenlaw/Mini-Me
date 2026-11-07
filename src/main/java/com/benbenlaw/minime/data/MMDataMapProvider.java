package com.benbenlaw.minime.data;

import com.benbenlaw.minime.datamaps.MMDataMaps;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DataMapProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MMDataMapProvider extends DataMapProvider {
    public MMDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(MMDataMaps.INSTRUCTION_COSTS)
                .add(id(Items.DIAMOND_PICKAXE), List.of(MiniMeTask.MINE), false)
                .add(id(Items.DIAMOND_AXE), List.of(MiniMeTask.CHOP_TREES, MiniMeTask.REPLANT), false)
                .add(id(Items.DIAMOND_SWORD), List.of(MiniMeTask.COMBAT), false)
                .add(Tags.Items.TOOLS_SHEAR, List.of(MiniMeTask.SHEAR), false)
                .add(id(Items.HOPPER), List.of(MiniMeTask.COLLECT), false)
                .add(id(Items.CHEST), List.of(MiniMeTask.DUMP), false)
                .add(id(Items.CRAFTING_TABLE), List.of(MiniMeTask.FACTORY), false)
        ;
    }

    private static Identifier id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
