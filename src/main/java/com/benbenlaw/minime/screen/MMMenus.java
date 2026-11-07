package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.MiniMe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MMMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MiniMe.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MiniMeMenu>> MINI_ME_MENU = registerMenuType("mini_me_menu", MiniMeMenu::create);

    public static final DeferredHolder<MenuType<?>, MenuType<InstructionDeskMenu>> INSTRUCTION_DESK_MENU =
            MENUS.register("instruction_desk_menu", () -> IMenuTypeExtension.create(InstructionDeskMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MiniMeFactoryMenu>> FACTORY_MENU =
            MENUS.register("mini_me_factory_menu", () -> IMenuTypeExtension.create(MiniMeFactoryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FactoryOrdersMenu>> FACTORY_ORDERS_MENU =
            MENUS.register("mini_me_factory_orders_menu", () -> IMenuTypeExtension.create(FactoryOrdersMenu::new));

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
