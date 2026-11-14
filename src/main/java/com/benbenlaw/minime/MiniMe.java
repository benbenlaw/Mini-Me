package com.benbenlaw.minime;

import com.benbenlaw.minime.config.MiniMeClientConfig;
import com.benbenlaw.minime.config.MiniMeConfig;
import com.benbenlaw.minime.datamaps.MMDataMaps;
import com.benbenlaw.minime.entity.MMEntities;
import com.benbenlaw.minime.block.MMBlocks;
import com.benbenlaw.minime.block.entity.MMBlockEntities;
import com.benbenlaw.minime.item.MMCreativeTab;
import com.benbenlaw.minime.network.MMNetworking;
import com.benbenlaw.minime.item.MMDataComponents;
import com.benbenlaw.minime.item.MMItems;
import com.benbenlaw.minime.screen.InstructionDeskScreen;
import com.benbenlaw.minime.screen.MMMenus;
import com.benbenlaw.minime.screen.MiniMeFactoryScreen;
import com.benbenlaw.minime.screen.MiniMeScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MiniMe.MOD_ID)
public class MiniMe {
    public static final String MOD_ID = "minime";
    public static final Logger LOGGER = LogManager.getLogger();

    public MiniMe(final IEventBus eventBus, final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MiniMeConfig.SPEC, "bbl/minime-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, MiniMeClientConfig.SPEC, "bbl/minime-client.toml");

        MMEntities.ENTITIES.register(eventBus);
        MMItems.ITEMS.register(eventBus);
        MMBlocks.BLOCKS.register(eventBus);
        MMBlockEntities.BLOCK_ENTITIES.register(eventBus);
        MMDataComponents.COMPONENTS.register(eventBus);
        MMCreativeTab.TABS.register(eventBus);
        eventBus.addListener(MiniMe::registerDataMaps);
        eventBus.addListener(MiniMe::registerPayloads);
        MMMenus.MENUS.register(eventBus);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        MMNetworking.register(event);
    }

    private static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(MMDataMaps.INSTRUCTION_COSTS);
    }

    @EventBusSubscriber(modid = MiniMe.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(MMMenus.MINI_ME_MENU.get(), MiniMeScreen::new);
            event.register(MMMenus.INSTRUCTION_DESK_MENU.get(), InstructionDeskScreen::new);
            event.register(MMMenus.FACTORY_MENU.get(), MiniMeFactoryScreen::new);
        }
    }

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
