package com.benbenlaw.minime.entity;

import com.benbenlaw.minime.MiniMe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class MMEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MiniMe.MOD_ID);

    public static final Supplier<EntityType<MiniMeEntity>> MINI_ME =
            ENTITIES.register("mini_me", () ->
                    EntityType.Builder.of(MiniMeEntity::new, MobCategory.CREATURE)
                            .sized(0.5f, 1.0f)
                            .build("mini_me")
            );
}