package example;

import xyz.synse.udppacketer.common.Connection;
import xyz.synse.udppacketer.common.IListener;
import xyz.synse.udppacketer.common.packets.Packet;

public class ServerListener implements IListener {
    @Override
    public void connected(Connection connection) {
        System.out.println("[SERVER] Client Connected " + connection.toString());
    }

    @Override
    public void disconnected(Connection connection, String reason) {
        System.out.println("[SERVER] Client Disconnected " + connection.toString() + " Reason: " + reason);
    }

    @Override
    public void received(Packet packet, Connection connection) {
        System.out.println("[SERVER] Received " + packet.toString() + " from " + connection.toString());
    }

    @Override
    public void sent(Packet packet, Connection connection) {
        System.out.println("[SERVER] Sent " + packet.toString() + " from " + connection.toString());
    }
}
