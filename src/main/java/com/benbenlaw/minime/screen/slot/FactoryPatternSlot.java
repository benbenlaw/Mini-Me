package com.benbenlaw.minime.screen.slot;

import com.benbenlaw.core.screen.util.slot.FilterSlot;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class FactoryPatternSlot extends FilterSlot {
    public FactoryPatternSlot(ResourceHandler<ItemResource> handler, IndexModifier<ItemResource> modifier, int index, int x, int y) {
        super(handler, modifier, index, x, y);
    }
}
