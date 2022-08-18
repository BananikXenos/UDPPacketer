package com.github.bananikxenos.udppacketer.threads.server;

import com.github.bananikxenos.udppacketer.UDPServer;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.headers.PacketHeader;
import com.github.bananikxenos.udppacketer.packets.headers.PacketsSendMode;
import com.github.bananikxenos.udppacketer.utils.Compression;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;

public class ServerSendThread extends Thread {
    private final UDPServer udpServer;

    private final ArrayList<PendingPacket> pendingPackets = new ArrayList<>();

    public ServerSendThread(UDPServer udpServer) {
        this.udpServer = udpServer;
    }

    @Override
    public void run() {
        while (udpServer != null && udpServer.getSocket() != null && !udpServer.getSocket().isClosed()) {
            if (this.udpServer.getPacketsSendMode() == PacketsSendMode.POLL && !this.pendingPackets.isEmpty()) {
                sendPacket(pendingPackets.remove(0));
            }
        }
    }

    private void sendPacketNewThread(PendingPacket pendingPacket) {
        new Thread(() -> sendPacket(pendingPacket)).start();
    }

    private void sendPacket(PendingPacket pendingPacket) {
        try {
            // Create output stream to write data to
            ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream(udpServer.getBufferSize());
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            dataOutputStream.writeInt(PacketHeader.PACKET.ordinal());

            // Write the packet id
            dataOutputStream.writeInt(udpServer.getPacketProtocol().getClientboundId(pendingPacket.packet()));

            // Write the packet data to output stream
            pendingPacket.packet().write(dataOutputStream);

            // Gets the bytes from output stream to send
            byte[] msg = bufferedOutputStream.toByteArray();

            //Check if to use Compression
            if (udpServer.isCompression()) {
                // Compress
                msg = Compression.compress(msg);
            }

            // Sends the data
            DatagramPacket p = new DatagramPacket(msg, msg.length, pendingPacket.address(), pendingPacket.port());
            udpServer.getSocket().send(p);
        } catch (IOException ignored) {
        }
    }

    public void sendPacket(byte[] data, InetAddress address, int port) {
        try {

            // Gets the bytes from output stream to send
            byte[] msg = data;

            //Check if to use Compression
            if (udpServer.isCompression()) {
                // Compress
                msg = Compression.compress(msg);
            }

            // Sends the data
            DatagramPacket p = new DatagramPacket(msg, msg.length, address, port);
            udpServer.getSocket().send(p);
        } catch (IOException ignored) {
        }
    }

    public void addToSending(Packet packet, InetAddress address, int port) {
        if (this.udpServer.getPacketsSendMode() == PacketsSendMode.POLL)
            pendingPackets.add(new PendingPacket(packet, address, port));
        else
            sendPacketNewThread(new PendingPacket(packet, address, port));
    }

    record PendingPacket(Packet packet, InetAddress address, int port) {
    }
}
