package com.benbenlaw.minime.event;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.entity.MMEntities;
import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = MiniMe.MOD_ID)
public class SummonMiniMe {

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        BlockState belowState = event.getLevel().getBlockState(pos.below());

        if (state.is(Blocks.PLAYER_HEAD) && belowState.is(Blocks.GOLD_BLOCK)) {

            MiniMeEntity miniMe = MMEntities.MINI_ME.get().create(event.getLevel());
            assert miniMe != null;

            miniMe.setPos(pos.getX() + 0.5, pos.getY() - 1, pos.getZ() + 0.5);
            miniMe.setHome(pos.below(), 8);
            miniMe.setOwnerUUID(event.getEntity().getUUID());

            double dx = event.getEntity().getX() - miniMe.getX();
            double dz = event.getEntity().getZ() - miniMe.getZ();

            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);

            miniMe.yBodyRot = yaw;
            miniMe.yHeadRot = yaw;

            event.getLevel().addFreshEntity(miniMe);

            event.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            event.getLevel().setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());

        }
    }
}
