package com.benbenlaw.minime.data;

import com.benbenlaw.minime.block.MMBlocks;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class MMLootTableProvider extends VanillaBlockLoot {
    private final Set<Block> knownBlocks = new ReferenceOpenHashSet<>();

    public MMLootTableProvider(HolderLookup.Provider provider) {
        super(provider);
    }

    @Override
    protected void generate() {
        this.dropSelf(MMBlocks.INSTRUCTION_DESK.get());
        this.dropSelf(MMBlocks.MINI_ME_FACTORY.get());
        this.dropSelf(MMBlocks.MINI_ME_CONSTRUCTOR.get());
    }

    @Override
    protected void add(@NotNull Block block, @NotNull LootTable.Builder table) {
        super.add(block, table);
        knownBlocks.add(block);
    }

    @NotNull
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return knownBlocks;
    }
}
