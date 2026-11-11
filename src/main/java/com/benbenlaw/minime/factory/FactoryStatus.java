package com.benbenlaw.minime.factory;

public enum FactoryStatus {
    IDLE("Idle."),
    NO_ORDERS("No orders yet. Drop an item on the slot at the left, then use the config button to set how many to keep."),
    NO_RECIPE("No crafting recipe makes the items still needed."),
    NO_WORKER("No Mini Me with the Factory job is working here."),
    NO_STORAGE("Put a chest or barrel in the factory's area."),
    WORKING("Working."),
    WAITING("Waiting for ingredients."),
    STOCKED("Every stock level is met.");

    public final String text;

    FactoryStatus(String text) {
        this.text = text;
    }

    public static FactoryStatus of(int ordinal) {
        FactoryStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
    }
}
