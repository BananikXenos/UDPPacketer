package com.github.bananikxenos.udppacketer.threads.server;

import com.github.bananikxenos.udppacketer.UDPServer;
import com.github.bananikxenos.udppacketer.connections.Connection;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.headers.PacketHeader;
import com.github.bananikxenos.udppacketer.utils.Compression;

import java.io.*;
import java.net.DatagramPacket;
import java.util.Optional;

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

                    Optional<Connection> optional = udpServer.getConnections().stream().filter(con -> con.getAddress() == packet.getAddress() && con.getPort() == packet.getPort()).findFirst();
                    Connection connection = optional.orElse(null);

                    if (packetHeader == PacketHeader.CONNECT) {
                        if(optional.isEmpty()){
                            udpServer.getConnections().add(connection =new Connection(packet.getAddress(), packet.getPort()));

                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                            dataOutputStream.writeInt(PacketHeader.CONNECTED.ordinal());

                            this.udpServer.getServerSendThread().sendPacket(byteArrayOutputStream.toByteArray(), packet.getAddress(), packet.getPort());

                            // Check if listener isn't null & packet isn't null
                            if (udpServer.getListener() != null)
                                udpServer.getListener().onClientConnected(connection); // Execute the listener
                        }else{
                            System.out.println("SERVER > Client tried to connect but he is already connected.");
                        }
                    }else if(packetHeader == PacketHeader.PACKET){
                        if(optional.isEmpty()) {
                            System.out.println("SERVER > Received packet but client not connected.");
                            return;
                        }

                        // Construct the packet
                        Packet pPacket = udpServer.getPacketProtocol().createClientboundPacket(dataInputStream.readInt(), dataInputStream);

                        // Check if listener isn't null & packet isn't null
                        if (udpServer.getListener() != null && pPacket != null)
                            udpServer.getListener().onPacketReceived(pPacket, connection); // Execute the listener
                    }else if(packetHeader == PacketHeader.RTT_REPLY){
                        if(optional.isEmpty()) {
                            System.out.println("SERVER > Received ping but client not connected.");
                            return;
                        }

                        double ping = System.currentTimeMillis() - dataInputStream.readLong();
                        connection.setSmoothRTT(ping);
                        connection.getDisconnectTimer().reset();
                    }else if(packetHeader == PacketHeader.RTT_REQUEST){
                        if(optional.isEmpty()) {
                            System.out.println("SERVER > Received ping request but client not connected.");
                            return;
                        }

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                        dataOutputStream.writeInt(PacketHeader.RTT_REPLY.ordinal());

                        dataOutputStream.writeLong(dataInputStream.readLong());

                        this.udpServer.getServerSendThread().sendPacket(byteArrayOutputStream.toByteArray(), packet.getAddress(), packet.getPort());
                    }else if(packetHeader == PacketHeader.DISCONNECT){
                        if(optional.isEmpty()) {
                            System.out.println("SERVER > Received disconnect but client isn't already connected.");
                            return;
                        }

                        udpServer.getConnections().remove(connection);

                        if (udpServer.getListener() != null)
                            udpServer.getListener().onClientDisconnect(connection); // Execute the listener
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }
}
