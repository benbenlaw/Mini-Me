package com.benbenlaw.minime.event.client;

import com.benbenlaw.core.util.TooltipUtil;
import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.block.MMBlocks;
import com.benbenlaw.minime.entity.MiniMeTask;
import com.benbenlaw.minime.item.MMDataComponents;
import com.benbenlaw.minime.item.MMItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = MiniMe.MOD_ID, value = Dist.CLIENT)
public class TooltipEvent {
    @SubscribeEvent
    public static void onTooltipEvent(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        TooltipUtil.addShiftTooltip(stack, event, MMBlocks.INSTRUCTION_DESK.get().asItem(), "tooltip.minime.instruction_desk");

        if (!stack.is(MMItems.INSTRUCTIONS.get())) return;

        List<MiniMeTask> tasks = stack.getOrDefault(MMDataComponents.INSTRUCTION_TASKS.get(), List.of());

        if (tasks.isEmpty()) {
            TooltipUtil.addShiftTooltip(stack, event, MMItems.INSTRUCTIONS.get(), "tooltip.minime.instructions");
            return;
        }

        List<Component> tooltip = event.getToolTip();
        if (Minecraft.getInstance().hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.minime.instructions_header").withStyle(ChatFormatting.BLUE));
            for (MiniMeTask task : tasks) {
                tooltip.add(Component.literal("* " + task.label()).withStyle(ChatFormatting.BLUE));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.bblcore.shift").withStyle(ChatFormatting.YELLOW));
        }
    }
}
