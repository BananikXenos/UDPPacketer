package com.github.bananikxenos.udppacketer.listener;

import com.github.bananikxenos.udppacketer.connections.Connection;
import com.github.bananikxenos.udppacketer.packets.Packet;

import java.net.InetAddress;

public interface ClientListener {
    void onPacketReceived(Packet packet);

    void onConnected();

    void onDisconnected();
}
