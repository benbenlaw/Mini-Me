package com.benbenlaw.minime.compat;

import net.neoforged.fml.ModList;

public final class NeoSyncCompat {
    public static final String MOD_ID = "neosync";

    private NeoSyncCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }
}
