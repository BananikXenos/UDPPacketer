package com.github.bananikxenos.udppacketer.packets;

/**
 * Represents a definition of a packet with various
 * information about it, such as its id, class and
 * factory for construction.
 *
 * @param <T> the packet type
 */
public class PacketDefinition<T extends Packet> {
    private final int id;
    private final Class<T> packetClass;

    public PacketDefinition(final int id, final Class<T> packetClass) {
        this.id = id;
        this.packetClass = packetClass;
    }

    /**
     * Returns the id of the packet.
     *
     * @return the id of the packet
     */
    public int getId() {
        return this.id;
    }

    /**
     * Returns the class of the packet.
     *
     * @return the class of the packet
     */
    public Class<T> getPacketClass() {
        return this.packetClass;
    }
}
