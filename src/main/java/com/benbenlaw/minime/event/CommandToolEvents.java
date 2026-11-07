package com.benbenlaw.minime.event;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.item.CommandToolItem;
import com.benbenlaw.minime.item.MMDataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = MiniMe.MOD_ID)
public class CommandToolEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof CommandToolItem)) return;

        Player player = event.getEntity();
        if (player.isSecondaryUseActive()) return;

        boolean bound = stack.get(MMDataComponents.COMMAND_TARGET.get()) != null;
        if (bound) {
            event.setUseBlock(TriState.FALSE);
        }
    }
}
