package com.benbenlaw.minime.integration.jei;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.screen.MiniMeFactoryScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class MiniMeJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return MiniMe.identifier( "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(MiniMeFactoryScreen.class, 87, 114, 25, 15, RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new FactoryTransferHandler(registration.getTransferHelper()), RecipeTypes.CRAFTING);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        FactoryJeiSync.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        FactoryJeiSync.setRuntime(null);
    }
}
