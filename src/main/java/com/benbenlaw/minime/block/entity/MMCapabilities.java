package com.benbenlaw.minime.block.entity;

import com.benbenlaw.minime.MiniMe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = MiniMe.MOD_ID)
public class MMCapabilities {
    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MMBlockEntities.MINI_ME_CONSTRUCTOR.get(), (constructor, side) -> constructor.energyHandler());
    }
}
