package com.github.bananikxenos.udppacketer.threads.client;

import com.github.bananikxenos.udppacketer.UDPClient;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.utils.Compression;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;

public class ClientReceiveThread extends Thread {
    private final UDPClient udpClient;

    public ClientReceiveThread(UDPClient udpClient) {
        this.udpClient = udpClient;
    }

    @Override
    public void run() {
        // Loop the receiving
        while (!udpClient.getSocket().isClosed()) {
            // Receive the packet
            DatagramPacket packet = new DatagramPacket(udpClient.getBuffer(), udpClient.getBufferSize());
            try {
                udpClient.getSocket().receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            new Thread(() -> {
                byte[] data = new byte[udpClient.getBufferSize()];

                System.arraycopy(packet.getData(), 0, data, 0, udpClient.getBufferSize());

                // Check if compressed
                if (Compression.isCompressed(data)) {
                    // Decompress
                    data = Compression.decompress(data);
                }

                // Read bytes into Input
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

                // Construct the packet
                Packet pPacket = null;
                try {
                    pPacket = udpClient.getPacketProtocol().createServerboundPacket(dataInputStream.readInt(), dataInputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Check if listener isn't null & packet isn't null
                if (udpClient.getListener() != null && pPacket != null)
                    udpClient.getListener().onPacketReceived(pPacket, packet.getAddress(), packet.getPort()); // Execute the listener
            }).start();
        }
    }
}
