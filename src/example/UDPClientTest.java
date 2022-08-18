package example;

import com.github.bananikxenos.udppacketer.UDPClient;
import com.github.bananikxenos.udppacketer.listener.UDPNetworkingListener;
import com.github.bananikxenos.udppacketer.packets.Packet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPClientTest {
    private UDPClient client;

    public UDPClientTest() throws SocketException, UnknownHostException {
        this.client = new UDPClient(new ExampleProtocol(), new UDPNetworkingListener() {
            @Override
            public void onPacketReceived(Packet packet, InetAddress address, int port) {
                if(packet instanceof TestPacket testPacket){
                    System.out.print("CLIENT >> Received example.Test Packet With coolString: " + testPacket.getCoolText() + " and Time: " + testPacket.getCurrentTime());
                }
            }
        });

        this.client.connect("127.0.0.1", 4425);

        try {
            this.client.send(new TestPacket("Hello, World!", System.currentTimeMillis()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
