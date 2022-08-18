package com.github.bananikxenos.udppacketer.threads.server;

import com.github.bananikxenos.udppacketer.UDPServer;
import com.github.bananikxenos.udppacketer.connections.Connection;
import com.github.bananikxenos.udppacketer.packets.headers.PacketHeader;
import com.github.bananikxenos.udppacketer.utils.Timer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class ServerRttThread extends Thread {
    private final UDPServer udpServer;

    public ServerRttThread(UDPServer udpServer) {
        this.udpServer = udpServer;
    }

    @Override
    public void run() {
        while (udpServer != null && udpServer.getSocket() != null && !udpServer.getSocket().isClosed()) {
            for (Connection connection : udpServer.getConnections()) {
                if (connection.getRttSendTimer().hasElapsed(5_000L, true)) {
                    try {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                        dataOutputStream.writeInt(PacketHeader.RTT_REQUEST.ordinal());
                        dataOutputStream.writeLong(System.currentTimeMillis());

                        udpServer.getServerSendThread().sendPacket(byteArrayOutputStream.toByteArray(), connection.getAddress(), connection.getPort());
                    } catch (Exception ignored) {
                    }
                }

                if (connection.getDisconnectTimer().hasElapsed(udpServer.getClientTimeout(), false)) {
                    try {
                        udpServer.getConnections().remove(connection);

                        if (udpServer.getListener() != null)
                            udpServer.getListener().onClientDisconnect(connection); // Execute the listener

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                        dataOutputStream.writeInt(PacketHeader.DISCONNECT.ordinal());

                        udpServer.getServerSendThread().sendPacket(byteArrayOutputStream.toByteArray(), connection.getAddress(), connection.getPort());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}
