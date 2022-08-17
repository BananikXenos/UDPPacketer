package com.github.bananikxenos.udppacketer.packets;

import java.io.DataOutputStream;
import java.io.IOException;

public interface Packet {
    /**
     * Writes the packet to the given output buffer.
     *
     * @param out The output destination to write to.
     * @throws IOException If an I/O error occurs.
     */
    void write(DataOutputStream out) throws IOException;
}
