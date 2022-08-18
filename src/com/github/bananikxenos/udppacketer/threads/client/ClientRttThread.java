package com.github.bananikxenos.udppacketer.threads.client;

import com.github.bananikxenos.udppacketer.UDPClient;
import com.github.bananikxenos.udppacketer.packets.headers.PacketHeader;
import com.github.bananikxenos.udppacketer.packets.headers.PacketsSendMode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientRttThread extends Thread {
    private final UDPClient udpClient;

    public ClientRttThread(UDPClient udpClient) {
        this.udpClient = udpClient;
    }

    @Override
    public void run() {
        while (udpClient != null && udpClient.getSocket() != null && !udpClient.getSocket().isClosed()) {
            if (udpClient.getRttSendTimer().hasElapsed(5_000L, true)) {
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                    dataOutputStream.writeInt(PacketHeader.RTT_REQUEST.ordinal());
                    dataOutputStream.writeLong(System.currentTimeMillis());

                    udpClient.getClientSendThread().sendPacket(byteArrayOutputStream.toByteArray());
                }catch (Exception ignored){}
            }

            if(udpClient.getDisconnectTimer().hasElapsed(udpClient.getDisconnectTime(), false)){
                try {
                    this.udpClient.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
