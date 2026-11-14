package com.benbenlaw.minime.network;

import com.benbenlaw.minime.MiniMe;
import com.benbenlaw.minime.network.packet.CommandToolPacket;
import com.benbenlaw.minime.network.packet.CustomizePacket;
import com.benbenlaw.minime.network.packet.FactoryClickPacket;
import com.benbenlaw.minime.network.packet.FactoryFillPacket;
import com.benbenlaw.minime.network.packet.FactoryViewPacket;
import com.benbenlaw.minime.network.packet.MiniMeActionPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class MMNetworking {

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MiniMe.MOD_ID);

        //Server to Client
        registrar.playToClient(FactoryViewPacket.TYPE, FactoryViewPacket.STREAM_CODEC, FactoryViewPacket.HANDLER);

        //Client to Server
        registrar.playToServer(CommandToolPacket.TYPE, CommandToolPacket.STREAM_CODEC, CommandToolPacket.HANDLER);
        registrar.playToServer(FactoryClickPacket.TYPE, FactoryClickPacket.STREAM_CODEC, FactoryClickPacket.HANDLER);
        registrar.playToServer(MiniMeActionPacket.TYPE, MiniMeActionPacket.STREAM_CODEC, MiniMeActionPacket.HANDLER);
        registrar.playToServer(FactoryFillPacket.TYPE, FactoryFillPacket.STREAM_CODEC, FactoryFillPacket.HANDLER);
        registrar.playToServer(CustomizePacket.TYPE, CustomizePacket.STREAM_CODEC, CustomizePacket.HANDLER);
    }
}
