package com.benbenlaw.minime.event.client;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.entity.MMEntities;
import com.benbenlaw.minime.entity.renderer.MiniMeRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MiniMe.MOD_ID, value = Dist.CLIENT)
public class ClientRenderer {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                MMEntities.MINI_ME.get(),
                MiniMeRenderer::new
        );
    }
}
