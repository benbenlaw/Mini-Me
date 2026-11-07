package com.benbenlaw.minime.item;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.MMBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class MMCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MiniMe.MOD_ID);

    public static final Supplier<CreativeModeTab> MINI_ME_TAB = TABS.register("minime", () -> CreativeModeTab.builder()
            .icon(() -> MMBlocks.INSTRUCTION_DESK.get().asItem().getDefaultInstance())
            .title(Component.translatable("itemGroup.minime"))
            .displayItems((parameters, output) -> MMItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());
}
