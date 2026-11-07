package com.benbenlaw.minime.item;

import com.benbenlaw.minime.client.CommandToolClient;
import com.benbenlaw.minime.entity.AreaTarget;
import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class CommandToolItem extends Item {
    public CommandToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean bind(ServerPlayer player, MiniMeEntity miniMe, AreaTarget area) {
        ItemStack tool = findTool(player);
        if (tool.isEmpty()) {
            player.sendSystemMessage(Component.literal("You need a Mini Me Command Tool in your inventory."), true);
            return false;
        }
        tool.set(MMDataComponents.COMMAND_TARGET.get(), new CommandTarget(miniMe.getUUID(), area));
        player.sendSystemMessage(Component.literal("Right-click a block to centre the " + area.label.toLowerCase()
                + " of " + miniMe.getName().getString() + " on it."), true);
        return true;
    }

    private static ItemStack findTool(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof CommandToolItem) return held;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof CommandToolItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            CommandToolClient.open();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player user = context.getPlayer();
        CommandTarget target = context.getItemInHand().get(MMDataComponents.COMMAND_TARGET.get());
        if (user == null || user.isSecondaryUseActive()) return InteractionResult.PASS;

        if (target == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level) || !(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        if (!(level.getEntity(target.miniMe()) instanceof MiniMeEntity miniMe) || miniMe.getHomePos() == null) {
            player.sendSystemMessage(Component.literal("That Mini Me is not in this dimension or has no home."), true);
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        BlockPos home = miniMe.getHomePos();
        miniMe.setSettingValue(target.area().offsetX, clicked.getX() - home.getX());
        miniMe.setSettingValue(target.area().offsetZ, clicked.getZ() - home.getZ());

        int x = miniMe.getSetting(target.area().offsetX);
        int z = miniMe.getSetting(target.area().offsetZ);
        player.sendSystemMessage(Component.literal(target.area().label + " centred, offset " + x + ", " + z + " from home."), true);
        return InteractionResult.SUCCESS;
    }
}
