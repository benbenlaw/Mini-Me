package com.benbenlaw.minime.screen;

import com.benbenlaw.minime.config.MiniMeClientConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class FactorySettings {
    public record Toggle(String label, String tooltip, ModConfigSpec.BooleanValue value) {
        public boolean get() {
            return value.get();
        }

        public void flip() {
            value.set(!value.get());
            value.save();
        }
    }

    public static final Toggle JEI_SYNC = new Toggle(
            "JEI search sync",
            "Keep the search bar and JEI's search bar showing the same text.",
            MiniMeClientConfig.FACTORY_JEI_SYNC);

    public static final Toggle AUTO_FOCUS = new Toggle(
            "Auto focus search",
            "Select the search bar as soon as the screen opens so you can type straight away.",
            MiniMeClientConfig.FACTORY_AUTO_FOCUS);

    public static final List<Toggle> TOGGLES = List.of(AUTO_FOCUS, JEI_SYNC);

    private FactorySettings() {
    }
}
