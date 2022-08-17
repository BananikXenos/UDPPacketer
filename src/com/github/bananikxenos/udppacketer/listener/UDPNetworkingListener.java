package com.github.bananikxenos.udppacketer.listener;

import com.github.bananikxenos.udppacketer.packets.Packet;

import java.net.InetAddress;

public interface UDPNetworkingListener {
    /**
     * Called when packet is received
     * @param packet The Packet
     * @param address Address from sender
     * @param port The port from sender
     */
    void onPacketReceived(Packet packet, InetAddress address, int port);
}
