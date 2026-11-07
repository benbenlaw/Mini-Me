package com.benbenlaw.minime.block;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.item.MMItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class MMBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MiniMe.MOD_ID);

    public static final DeferredBlock<Block> INSTRUCTION_DESK = registerBlock("instruction_desk",
            properties -> new InstructionDeskBlock(properties
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<Block> MINI_ME_FACTORY = registerBlock("mini_me_factory",
            properties -> new MiniMeFactoryBlock(properties
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MINI_ME_CONSTRUCTOR = registerBlock("mini_me_constructor",
            properties -> new MiniMeConstructorBlock(properties
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> block = BLOCKS.registerBlock(name, function);
        MMItems.ITEMS.registerItem(name, properties -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
        return block;
    }
}
