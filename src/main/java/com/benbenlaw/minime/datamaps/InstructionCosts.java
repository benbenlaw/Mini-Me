package com.benbenlaw.minime.datamaps;

import com.benbenlaw.minime.entity.MiniMeTask;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InstructionCosts {
    public record Entry(List<Item> items, List<MiniMeTask> tasks) {
    }

    public static List<Entry> all() {
        Map<ResourceKey<Item>, List<MiniMeTask>> map = BuiltInRegistries.ITEM.getDataMap(MMDataMaps.INSTRUCTION_COSTS);

        Map<List<MiniMeTask>, List<Item>> grouped = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    Item item = BuiltInRegistries.ITEM.getValue(entry.getKey());
                    if (item == Items.AIR || entry.getValue().isEmpty()) return;

                    List<MiniMeTask> tasks = entry.getValue().stream().distinct().sorted().toList();
                    grouped.computeIfAbsent(tasks, key -> new ArrayList<>()).add(item);
                });

        List<Entry> entries = new ArrayList<>();
        grouped.forEach((tasks, items) -> entries.add(new Entry(items, tasks)));
        entries.sort(Comparator.comparing((Entry entry) -> entry.tasks().getFirst()).thenComparing(entry -> entry.tasks().size()));
        return entries;
    }
}
