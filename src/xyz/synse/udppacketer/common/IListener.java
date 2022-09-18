package xyz.synse.udppacketer.common;

import xyz.synse.udppacketer.common.packets.Packet;

public interface IListener {
    void connected(Connection connection);
    void disconnected(Connection connection, String reason);
    void received(Packet packet, Connection connection);
    void sent(Packet packet, Connection connection);
}
