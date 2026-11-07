package com.benbenlaw.minime.data;

import com.benbenlaw.minime.MiniMe;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class MMLangProvider extends LanguageProvider {
    public MMLangProvider(PackOutput output) {
        super(output, MiniMe.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.minime", "Mini Me");

        add("entity.minime.mini_me", "Mini Me");

        add("block.minime.instruction_desk", "Instruction Desk");
        add("block.minime.mini_me_factory", "Mini Me Factory");
        add("block.minime.mini_me_constructor", "Mini Me Constructor");

        add("item.minime.instructions", "Mini Me Instructions");
        add("item.minime.command_tool", "Mini Me Command Tool");

        add("tooltip.minime.instructions", "Put in a Mini Me's off hand to give it jobs");
        add("tooltip.minime.instructions_header", "Instructions:");
        add("tooltip.minime.instruction_desk", "Insert Mini Me Instructions and pay items to write jobs onto them.");
    }
}
