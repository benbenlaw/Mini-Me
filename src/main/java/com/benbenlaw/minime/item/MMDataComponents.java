package com.benbenlaw.minime.item;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class MMDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, MiniMe.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CommandTarget>> COMMAND_TARGET =
            COMPONENTS.register("command_target", () ->
                    DataComponentType.<CommandTarget>builder()
                            .persistent(CommandTarget.CODEC)
                            .networkSynchronized(CommandTarget.STREAM_CODEC)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<MiniMeTask>>> INSTRUCTION_TASKS =
            COMPONENTS.register("instruction_tasks", () ->
                    DataComponentType.<List<MiniMeTask>>builder()
                            .persistent(MiniMeTask.CODEC.listOf())
                            .networkSynchronized(ByteBufCodecs.VAR_INT
                                    .map(index -> MiniMeTask.values()[index], MiniMeTask::ordinal)
                                    .apply(ByteBufCodecs.list()))
                            .cacheEncoding()
                            .build());
}
