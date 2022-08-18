package com.github.bananikxenos.udppacketer.threads.client;

import com.github.bananikxenos.udppacketer.UDPClient;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.headers.PacketHeader;
import com.github.bananikxenos.udppacketer.utils.Compression;

import java.io.*;
import java.net.DatagramPacket;
import java.nio.channels.AsynchronousCloseException;

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

                new Thread(() -> {
                    try {
                        byte[] data = packet.getData();

                        // Check if compressed
                        if (Compression.isCompressed(data)) {
                            // Decompress
                            data = Compression.decompress(data);
                        }

                        // Read bytes into Input
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

                        PacketHeader packetHeader = PacketHeader.values()[dataInputStream.readInt()];

                        if(packetHeader == PacketHeader.CONNECTED){
                            udpClient.setConnected(true);
                            if (udpClient.getListener() != null)
                                udpClient.getListener().onConnected(); // Execute the listener
                        } else if (packetHeader == PacketHeader.PACKET){
                            // Construct the packet
                            Packet pPacket = udpClient.getPacketProtocol().createServerboundPacket(dataInputStream.readInt(), dataInputStream);

                            // Check if listener isn't null & packet isn't null
                            if (udpClient.getListener() != null && pPacket != null)
                                udpClient.getListener().onPacketReceived(pPacket); // Execute the listener
                        } else if (packetHeader == PacketHeader.DISCONNECT) {
                            udpClient.setConnected(false);
                            udpClient.close();
                        } else if (packetHeader == PacketHeader.RTT_REPLY){
                            double ping = System.currentTimeMillis() - dataInputStream.readLong();
                            udpClient.setSmoothRTT(ping);
                            udpClient.getDisconnectTimer().reset();
                        } else if (packetHeader == PacketHeader.RTT_REQUEST){
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                            dataOutputStream.writeInt(PacketHeader.RTT_REPLY.ordinal());

                            dataOutputStream.writeLong(dataInputStream.readLong());

                            this.udpClient.getClientSendThread().sendPacket(byteArrayOutputStream.toByteArray());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IOException ignored) {

            }
        }
    }
}
