package com.benbenlaw.minime.compat.neosync;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.entity.MMBlockEntities;
import com.benbenlaw.minime.compat.NeoSyncCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MiniMe.MOD_ID, value = Dist.CLIENT)
public class NeoSyncClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (!NeoSyncCompat.isLoaded()) return;

        event.registerBlockEntityRenderer(MMBlockEntities.MINI_ME_CONSTRUCTOR.get(), NeoSyncConstructorRenderer::new);
    }
}
