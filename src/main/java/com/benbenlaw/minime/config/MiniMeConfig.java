package com.benbenlaw.minime.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MiniMeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DEFAULT_HOME_RADIUS;
    public static final ModConfigSpec.BooleanValue DAMAGE_TOOLS;
    public static final ModConfigSpec.IntValue MAX_TREE_LOGS;

    public static final ModConfigSpec.IntValue FACTORY_RADIUS;
    public static final ModConfigSpec.IntValue CONSTRUCTOR_ENERGY;
    public static final ModConfigSpec.DoubleValue CONSTRUCTOR_HEALTH_COST;
    public static final ModConfigSpec.BooleanValue CONSTRUCTOR_WARN_INSTEAD_OF_KILLING;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");
        DEFAULT_HOME_RADIUS = BUILDER
                .comment("Radius (in blocks) of the working area around a Mini Me's home.")
                .defineInRange("defaultHomeRadius", 8, 2, 32);
        DAMAGE_TOOLS = BUILDER
                .comment("Chopping, mining and shearing damage the tool, and a broken tool is lost.")
                .define("damageTools", true);
        MAX_TREE_LOGS = BUILDER
                .comment("Biggest tree (in logs) that will be chopped. Anything larger is assumed to be a build",
                        "that touches a tree and is left alone. Giant jungle and spruce trees need a few hundred.")
                .defineInRange("maxTreeLogs", 400, 20, 2000);
        BUILDER.pop();

        BUILDER.push("constructor");
        CONSTRUCTOR_ENERGY = BUILDER
                .comment("FE a Mini Me Constructor needs to build one Mini Me. Any energy source can power it, for example",
                        "a NeoSync treadmill placed against it.")
                .defineInRange("energyPerMiniMe", 20000, 100, 10000000);
        CONSTRUCTOR_HEALTH_COST = BUILDER
                .comment("Health points taken from the player who right-clicks a Mini Me Constructor to start a build.",
                        "20 is ten hearts, the same as NeoSync. 0 turns the cost off.")
                .defineInRange("healthCost", 20.0D, 0.0D, 1000.0D);
        CONSTRUCTOR_WARN_INSTEAD_OF_KILLING = BUILDER
                .comment("If the cost would kill the player, warn them and do nothing instead.")
                .define("warnInsteadOfKilling", false);
        BUILDER.pop();

        BUILDER.push("factory");
        FACTORY_RADIUS = BUILDER
                .comment("How far (in blocks, in every direction) from a Mini Me Factory its chests can be.",
                        "1 = the 3x3x3 cube around the factory.")
                .defineInRange("areaRadius", 1, 1, 3);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private MiniMeConfig() {
    }
}
