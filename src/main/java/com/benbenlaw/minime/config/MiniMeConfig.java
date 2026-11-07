package com.benbenlaw.minime.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MiniMeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DEFAULT_HOME_RADIUS;
    public static final ModConfigSpec.IntValue MAX_AREA_OFFSET;

    public static final ModConfigSpec.BooleanValue TREE_FARMING_ENABLED;
    public static final ModConfigSpec.BooleanValue REPLANT_SAPLINGS;
    public static final ModConfigSpec.BooleanValue DAMAGE_TOOLS;
    public static final ModConfigSpec.IntValue MAX_TREE_LOGS;

    public static final ModConfigSpec.BooleanValue MINING_ENABLED;
    public static final ModConfigSpec.BooleanValue MINING_DAMAGE_TOOLS;
    public static final ModConfigSpec.IntValue MAX_QUARRY_WIDTH;
    public static final ModConfigSpec.IntValue MINING_DEFAULT_DUMP_AFTER;

    public static final ModConfigSpec.BooleanValue SHEARING_ENABLED;
    public static final ModConfigSpec.BooleanValue SHEARING_DAMAGE_TOOLS;

    public static final ModConfigSpec.BooleanValue COMBAT_ENABLED;

    public static final ModConfigSpec.BooleanValue COLLECT_ITEMS;
    public static final ModConfigSpec.IntValue COLLECT_RADIUS;

    public static final ModConfigSpec.IntValue FACTORY_RADIUS;
    public static final ModConfigSpec.IntValue CONSTRUCTOR_ENERGY;
    public static final ModConfigSpec.DoubleValue CONSTRUCTOR_HEALTH_COST;
    public static final ModConfigSpec.BooleanValue CONSTRUCTOR_WARN_INSTEAD_OF_KILLING;

    public static final ModConfigSpec.BooleanValue DUMP_TO_CHEST;
    public static final ModConfigSpec.IntValue MAX_CHEST_AREA_SIZE;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");
        DEFAULT_HOME_RADIUS = BUILDER
                .comment("Radius (in blocks) of the working area around a Mini Me's home.")
                .defineInRange("defaultHomeRadius", 8, 2, 32);
        MAX_AREA_OFFSET = BUILDER
                .comment("How far (in blocks) the quarry or the chest search area can be moved away from the home block.")
                .defineInRange("maxAreaOffset", 32, 0, 128);
        BUILDER.pop();

        BUILDER.push("treeFarming");
        TREE_FARMING_ENABLED = BUILDER
                .comment("Chop down natural trees while holding an axe.")
                .define("enabled", true);
        REPLANT_SAPLINGS = BUILDER
                .comment("Replant saplings where a tree was chopped. Saplings are taken from the Mini Me's own inventory,",
                        "so they must be picked up (e.g. from decaying leaves) or put there by the player.")
                .define("replantSaplings", true);
        DAMAGE_TOOLS = BUILDER
                .comment("Chopping damages the axe, and a broken axe is lost.")
                .define("damageTools", true);
        MAX_TREE_LOGS = BUILDER
                .comment("Biggest tree (in logs) that will be chopped. Anything larger is assumed to be a build",
                        "that touches a tree and is left alone. Giant jungle and spruce trees need a few hundred.")
                .defineInRange("maxTreeLogs", 400, 20, 2000);
        BUILDER.pop();

        BUILDER.push("mining");
        MINING_ENABLED = BUILDER
                .comment("Dig a quarry while holding a pickaxe.")
                .define("enabled", true);
        MINING_DAMAGE_TOOLS = BUILDER
                .comment("Mining damages the pickaxe, and a broken pickaxe is lost.")
                .define("damageTools", true);
        MAX_QUARRY_WIDTH = BUILDER
                .comment("Longest side (in blocks) that can be chosen for the quarry in the task screen.")
                .defineInRange("maxQuarryWidth", 15, 1, 63);
        MINING_DEFAULT_DUMP_AFTER = BUILDER
                .comment("Starting value of a Mini Me's 'Dump after' setting (changeable in its Tasks screen): after",
                        "collecting this many items from mining it walks to a chest near home and empties its",
                        "inventory before carrying on. Needs the 'Use chest' job and a chest. 0 = only when full.")
                .defineInRange("defaultDumpAfterItems", 16, 0, 512);
        BUILDER.pop();

        BUILDER.push("shearing");
        SHEARING_ENABLED = BUILDER
                .comment("Shear sheep, mooshrooms and other shearable mobs while holding shears.")
                .define("enabled", true);
        SHEARING_DAMAGE_TOOLS = BUILDER
                .comment("Shearing damages the shears, and broken shears are lost.")
                .define("damageTools", true);
        BUILDER.pop();

        BUILDER.push("combat");
        COMBAT_ENABLED = BUILDER
                .comment("Attack nearby monsters while holding a sword.")
                .define("enabled", true);
        BUILDER.pop();

        BUILDER.push("loot");
        COLLECT_ITEMS = BUILDER
                .comment("Pick up dropped items near home.")
                .define("collectItems", true);
        COLLECT_RADIUS = BUILDER
                .comment("How far (in blocks) the Mini Me looks for dropped items.")
                .defineInRange("collectRadius", 10, 1, 32);
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
                .comment("How far (in blocks, in every direction) from a Mini Me Factory its chests and machines can be.",
                        "1 = the 3x3x3 cube around the factory.")
                .defineInRange("areaRadius", 1, 1, 3);
        BUILDER.pop();

        BUILDER.push("storage");
        DUMP_TO_CHEST = BUILDER
                .comment("Empty its inventory into a chest near home.")
                .define("dumpToChest", true);
        MAX_CHEST_AREA_SIZE = BUILDER
                .comment("Longest side (in blocks) that can be chosen for the area searched for a chest.")
                .defineInRange("maxChestAreaSize", 33, 1, 127);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private MiniMeConfig() {
    }
}
