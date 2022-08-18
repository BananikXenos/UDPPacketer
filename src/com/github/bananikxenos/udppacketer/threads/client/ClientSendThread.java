package com.github.bananikxenos.udppacketer.threads.client;

import com.github.bananikxenos.udppacketer.UDPClient;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.sending.PacketsSendMode;
import com.github.bananikxenos.udppacketer.utils.Compression;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.LinkedList;

public class ClientSendThread extends Thread {
    private final UDPClient udpClient;

    private final LinkedList<Packet> pendingPackets = new LinkedList<>();

    public ClientSendThread(UDPClient udpClient) {
        this.udpClient = udpClient;
    }

    @Override
    public void run() {
        while (udpClient != null && udpClient.getSocket() != null && !udpClient.getSocket().isClosed()) {
            if (this.udpClient.getPacketsSendMode() == PacketsSendMode.POLL && !this.pendingPackets.isEmpty()) {
                sendPacket(pendingPackets.poll());
            }
        }
    }

    private void sendPacketNewThread(Packet pendingPacket) {
        new Thread(() -> sendPacket(pendingPacket)).start();
    }

    private void sendPacket(Packet packet) {
        try {
            // Create output stream to write data to
            ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream(udpClient.getBufferSize());
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            // Write the packet id
            dataOutputStream.writeInt(udpClient.getPacketProtocol().getClientboundId(packet));

            // Write the packet data to output stream
            packet.write(dataOutputStream);

            // Gets the bytes from output stream to send
            byte[] msg = bufferedOutputStream.toByteArray();

            //Check if to use Compression
            if (udpClient.isCompression()) {
                // Compress
                msg = Compression.compress(msg);
            }

            // Sends the data
            DatagramPacket p = new DatagramPacket(msg, msg.length, udpClient.getAddress(), udpClient.getPort());
            udpClient.getSocket().send(p);
        } catch (IOException ignored) {}
    }

    public void addToSending(Packet packet) {
        if (this.udpClient.getPacketsSendMode() == PacketsSendMode.POLL)
            pendingPackets.add(packet);
        else
            sendPacketNewThread(packet);
    }
}
