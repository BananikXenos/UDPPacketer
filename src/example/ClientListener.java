package example;

import xyz.synse.udppacketer.common.Connection;
import xyz.synse.udppacketer.common.IListener;
import xyz.synse.udppacketer.common.packets.Packet;

public class ClientListener implements IListener {
    @Override
    public void connected(Connection connection) {
        System.out.println("[CLIENT] Connected to server");
    }

    @Override
    public void disconnected(Connection connection, String reason) {
        System.out.println("[CLIENT] Disconnected from server. Reason: " + reason);
    }

    @Override
    public void received(Packet packet, Connection connection) {
        System.out.println("[CLIENT] Received " + packet.toString());
    }

    @Override
    public void sent(Packet packet, Connection connection) {
        System.out.println("[CLIENT] Sent " + packet.toString());
    }
}
