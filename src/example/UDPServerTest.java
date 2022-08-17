package example;

import com.github.bananikxenos.udppacketer.UDPServer;
import com.github.bananikxenos.udppacketer.listener.UDPNetworkingListener;
import com.github.bananikxenos.udppacketer.packets.Packet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPServerTest {
    private UDPServer server;

    public UDPServerTest() throws SocketException {
        this.server = new UDPServer(new ExampleProtocol(), new UDPNetworkingListener() {
            @Override
            public void onPacketReceived(Packet packet, InetAddress address, int port) {
                if(packet instanceof TestPacket testPacket) {
                    System.out.println("SERVER >> Received example.Test Packet With coolString: " + testPacket.getCoolText() + " and Time: " + testPacket.getCurrentTime());

                    try {
                        server.send(address, port, new TestPacket("Hello Back!", System.currentTimeMillis()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        this.server.start(4425);
    }
}
