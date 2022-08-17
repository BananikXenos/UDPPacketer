package com.github.bananikxenos.udppacketer.packets;

import java.io.DataInputStream;
import java.io.IOException;

@FunctionalInterface
public interface PacketFactory<T extends Packet> {

    /**
     * Constructor of packet when it's received
     * @param input input data
     * @return Packet
     * @throws IOException throws Exception when reading
     */
    T construct(DataInputStream input) throws IOException;
}
