package com.benbenlaw.minime.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MiniMeClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue FACTORY_JEI_SYNC;
    public static final ModConfigSpec.BooleanValue FACTORY_AUTO_FOCUS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("factory");
        FACTORY_JEI_SYNC = BUILDER
                .comment("Keep the Mini Me Factory search bar and the JEI search bar in sync. Needs JEI.")
                .define("jeiSync", true);
        FACTORY_AUTO_FOCUS = BUILDER
                .comment("Select the Mini Me Factory search bar as soon as the screen opens.")
                .define("autoFocus", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private MiniMeClientConfig() {
    }
}
