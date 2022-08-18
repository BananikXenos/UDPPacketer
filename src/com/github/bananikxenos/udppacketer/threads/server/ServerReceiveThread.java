package com.github.bananikxenos.udppacketer.threads.server;

import com.github.bananikxenos.udppacketer.UDPServer;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.utils.Compression;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;

public class ServerReceiveThread extends Thread {
    private final UDPServer udpServer;

    public ServerReceiveThread(UDPServer udpServer) {
        this.udpServer = udpServer;
    }

    @Override
    public void run() {
        while (udpServer != null && udpServer.getSocket() != null && !udpServer.getSocket().isClosed()) {
            // Receive the packet
            DatagramPacket packet = new DatagramPacket(udpServer.getBuffer(), udpServer.getBufferSize());
            try {
                udpServer.getSocket().receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            new Thread(() -> {

                byte[] data = packet.getData();

                // Check if compressed
                if (Compression.isCompressed(data)) {
                    // Decompress
                    data = Compression.decompress(data);
                }

                // Read bytes into Input
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

                // Construct the packet
                Packet pPacket;
                try {
                    pPacket = udpServer.getPacketProtocol().createClientboundPacket(dataInputStream.readInt(), dataInputStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Check if listener isn't null & packet isn't null
                if (udpServer.getListener() != null && pPacket != null)
                    udpServer.getListener().onPacketReceived(pPacket, packet.getAddress(), packet.getPort()); // Execute the listener
            }).start();
        }
    }
}
