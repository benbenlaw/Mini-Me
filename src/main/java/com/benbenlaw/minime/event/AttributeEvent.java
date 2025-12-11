package com.benbenlaw.minime.event;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.entity.MMEntities;
import com.benbenlaw.minime.entity.MiniMeEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = MiniMe.MOD_ID)
public class AttributeEvent {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(MMEntities.MINI_ME.get(), MiniMeEntity.createAttributes().build());
    }


}
