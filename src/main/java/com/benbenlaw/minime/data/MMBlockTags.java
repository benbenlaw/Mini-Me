package com.benbenlaw.minime.data;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.MMBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class MMBlockTags extends BlockTagsProvider {
    MMBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, MiniMe.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(MMBlocks.INSTRUCTION_DESK.get())
        ;

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(MMBlocks.MINI_ME_FACTORY.get())
                .add(MMBlocks.MINI_ME_CONSTRUCTOR.get())
        ;

        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(MMBlocks.MINI_ME_FACTORY.get())
                .add(MMBlocks.MINI_ME_CONSTRUCTOR.get())
        ;
    }
}
