package com.benbenlaw.minime.datamaps;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import java.util.List;

public class MMDataMaps {
    public static final DataMapType<Item, List<MiniMeTask>> INSTRUCTION_COSTS = DataMapType.builder(
            MiniMe.identifier( "instruction_costs"), Registries.ITEM, MiniMeTask.CODEC.listOf())
            .synced(MiniMeTask.CODEC.listOf(), true).build();
}
