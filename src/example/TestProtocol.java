package example;

import xyz.synse.udppacketer.common.packets.PacketProtocol;

public class TestProtocol extends PacketProtocol {
    public TestProtocol(){
        register(1, TestPacket.class);
    }
}
