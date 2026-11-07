package com.benbenlaw.minime.item;

import com.benbenlaw.minime.MiniMe;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MMItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MiniMe.MOD_ID);

    public static final DeferredItem<Item> COMMAND_TOOL = ITEMS.registerItem("command_tool", CommandToolItem::new);

    public static final DeferredItem<Item> INSTRUCTIONS = ITEMS.registerSimpleItem("instructions", properties -> properties.stacksTo(1));
}
