package com.benbenlaw.minime.block.entity;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.MMBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class MMBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MiniMe.MOD_ID);

    public static final Supplier<BlockEntityType<InstructionDeskBlockEntity>> INSTRUCTION_DESK =
            BLOCK_ENTITIES.register("instruction_desk", () ->
                    new BlockEntityType<>(InstructionDeskBlockEntity::new, MMBlocks.INSTRUCTION_DESK.get()));

    public static final Supplier<BlockEntityType<MiniMeConstructorBlockEntity>> MINI_ME_CONSTRUCTOR =
            BLOCK_ENTITIES.register("mini_me_constructor", () ->
                    new BlockEntityType<>(MiniMeConstructorBlockEntity::new, MMBlocks.MINI_ME_CONSTRUCTOR.get()));

    public static final Supplier<BlockEntityType<MiniMeFactoryBlockEntity>> MINI_ME_FACTORY =
            BLOCK_ENTITIES.register("mini_me_factory", () ->
                    new BlockEntityType<>(MiniMeFactoryBlockEntity::new, MMBlocks.MINI_ME_FACTORY.get()));
}
