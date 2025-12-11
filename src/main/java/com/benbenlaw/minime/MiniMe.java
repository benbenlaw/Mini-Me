package com.benbenlaw.minime;


import com.benbenlaw.minime.entity.MMEntities;
import com.benbenlaw.minime.screen.MMMenus;
import com.benbenlaw.minime.screen.MiniMeScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MiniMe.MOD_ID)
public class MiniMe {
    public static final String MOD_ID = "minime";
    public static final Logger LOGGER = LogManager.getLogger();

    public MiniMe(final IEventBus eventBus, final ModContainer modContainer) {

        MMEntities.ENTITIES.register(eventBus);
        MMMenus.MENUS.register(eventBus);
    }

    @EventBusSubscriber(modid = MiniMe.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(MMMenus.MINI_ME_MENU.get(), MiniMeScreen::new);
        }
    }
}
