package com.benbenlaw.minime.client;

import com.benbenlaw.minime.screen.CommandToolScreen;
import net.minecraft.client.Minecraft;

public class CommandToolClient {
    public static void open() {
        Minecraft.getInstance().setScreen(new CommandToolScreen());
    }
}
