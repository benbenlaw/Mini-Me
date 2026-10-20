package com.benbenlaw.minime.entity;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

public enum MiniMeTask {

    COMBAT("Fight", ItemTags.SWORDS),
    CHOP_TREES("Chop trees", ItemTags.AXES),
    REPLANT("Replant", ItemTags.AXES),
    SHEAR("Shear", Tags.Items.TOOLS_SHEAR),
    MINE("Mine", ItemTags.PICKAXES),
    COLLECT("Collect", null),
    DUMP("Use chest", null);

    private final String label;
    @Nullable
    private final TagKey<Item> requiredTool;

    MiniMeTask(String label, @Nullable TagKey<Item> requiredTool) {
        this.label = label;
        this.requiredTool = requiredTool;
    }

    public String label() {
        return label;
    }

    public boolean isUsableWith(ItemStack mainHand) {
        return requiredTool == null || mainHand.is(requiredTool);
    }
}
