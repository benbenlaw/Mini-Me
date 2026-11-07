package com.benbenlaw.minime.compat.neosync;

import com.benbenlaw.minime.MiniMe;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@EventBusSubscriber(modid = MiniMe.MOD_ID, value = Dist.CLIENT)
public class ConstructorItemEvents {
    @SubscribeEvent
    public static void registerProperties(RegisterConditionalItemModelPropertyEvent event) {
        event.register(MiniMe.identifier( "neosync_loaded"), NeoSyncLoadedProperty.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerSpecial(RegisterSpecialModelRendererEvent event) {
        event.register(MiniMe.identifier( "constructor"), ConstructorSpecial.MAP_CODEC);
    }
}
