package com.benbenlaw.minime.integration.jei;

import com.benbenlaw.core.integration.jei.GhostFilter;
import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.screen.FactoryOrdersScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class MiniMeJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return MiniMe.identifier( "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(FactoryOrdersScreen.class, new GhostFilter<FactoryOrdersScreen>());
    }
}
