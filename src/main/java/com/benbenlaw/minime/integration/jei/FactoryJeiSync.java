package com.benbenlaw.minime.integration.jei;

import mezz.jei.api.runtime.IJeiRuntime;

public final class FactoryJeiSync {
    private static IJeiRuntime runtime;

    private FactoryJeiSync() {
    }

    static void setRuntime(IJeiRuntime value) {
        runtime = value;
    }

    public static String getText() {
        return runtime == null ? "" : runtime.getIngredientFilter().getFilterText();
    }

    public static void setText(String text) {
        if (runtime != null && !runtime.getIngredientFilter().getFilterText().equals(text)) {
            runtime.getIngredientFilter().setFilterText(text);
        }
    }
}
