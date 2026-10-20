package com.benbenlaw.minime.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common config (config/minime-common.toml). Each job the Mini Me does can be switched off here. */
public class MiniMeConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DEFAULT_HOME_RADIUS;

    public static final ModConfigSpec.BooleanValue TREE_FARMING_ENABLED;
    public static final ModConfigSpec.BooleanValue REPLANT_SAPLINGS;
    public static final ModConfigSpec.BooleanValue DAMAGE_TOOLS;
    public static final ModConfigSpec.IntValue MAX_TREE_LOGS;

    public static final ModConfigSpec.BooleanValue MINING_ENABLED;
    public static final ModConfigSpec.BooleanValue MINING_DAMAGE_TOOLS;
    public static final ModConfigSpec.IntValue MAX_QUARRY_WIDTH;
    public static final ModConfigSpec.IntValue MAX_QUARRY_DEPTH;
    public static final ModConfigSpec.IntValue MINING_DUMP_EVERY;

    public static final ModConfigSpec.BooleanValue SHEARING_ENABLED;
    public static final ModConfigSpec.BooleanValue SHEARING_DAMAGE_TOOLS;

    public static final ModConfigSpec.BooleanValue COMBAT_ENABLED;

    public static final ModConfigSpec.BooleanValue COLLECT_ITEMS;
    public static final ModConfigSpec.IntValue COLLECT_RADIUS;

    public static final ModConfigSpec.BooleanValue DUMP_TO_CHEST;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");
        DEFAULT_HOME_RADIUS = BUILDER
                .comment("Radius (in blocks) of the working area around a Mini Me's home.")
                .defineInRange("defaultHomeRadius", 8, 2, 32);
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
                .comment("Widest quarry (in blocks) that can be chosen in the task screen.",
                        "It is also limited to the width of the Mini Me's working area.")
                .defineInRange("maxQuarryWidth", 15, 1, 63);
        MAX_QUARRY_DEPTH = BUILDER
                .comment("How many layers deep a quarry goes, counting down from the layer under the home block.")
                .defineInRange("maxQuarryDepth", 16, 1, 256);
        MINING_DUMP_EVERY = BUILDER
                .comment("After mining this many blocks the Mini Me walks to the chest near home and empties its",
                        "inventory before carrying on (needs the 'Use chest' job and a chest). 0 = only when full.")
                .defineInRange("dumpEveryBlocks", 8, 0, 256);
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

        BUILDER.push("storage");
        DUMP_TO_CHEST = BUILDER
                .comment("Empty its inventory into a chest near home.")
                .define("dumpToChest", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private MiniMeConfig() {
    }
}
