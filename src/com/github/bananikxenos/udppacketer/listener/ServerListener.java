package com.github.bananikxenos.udppacketer.listener;

import com.github.bananikxenos.udppacketer.connections.Connection;
import com.github.bananikxenos.udppacketer.packets.Packet;

import java.net.InetAddress;

public interface ServerListener {
    void onPacketReceived(Packet packet, Connection connection);

    void onClientConnected(Connection connection);

    void onClientDisconnect(Connection connection);
}
