package example;

import com.github.bananikxenos.udppacketer.packets.PacketProtocol;

public class ExampleProtocol extends PacketProtocol {
    public ExampleProtocol(){
        register(1, TestPacket.class);
    }
}
