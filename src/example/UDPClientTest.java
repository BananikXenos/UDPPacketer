package example;

import com.github.bananikxenos.udppacketer.UDPClient;
import com.github.bananikxenos.udppacketer.connections.Connection;
import com.github.bananikxenos.udppacketer.listener.ClientListener;
import com.github.bananikxenos.udppacketer.listener.ServerListener;
import com.github.bananikxenos.udppacketer.packets.Packet;

import java.io.IOException;
import java.net.InetAddress;

public class UDPClientTest {
    private UDPClient client;

    public UDPClientTest() throws IOException, InterruptedException {
        this.client = new UDPClient(new ExampleProtocol(), new ClientListener() {
            @Override
            public void onPacketReceived(Packet packet) {
                if(packet instanceof TestPacket testPacket){
                    System.out.println("CLIENT >> Received example.Test Packet With coolString: " + testPacket.getCoolText() + " and Time: " + testPacket.getCurrentTime());
                }
            }

            @Override
            public void onConnected() {
                System.out.println("CLIENT >> Connected to server.");
            }

            @Override
            public void onDisconnected() {
                System.out.println("CLIENT >> Disconnected.");
            }
        });

        this.client.connect("127.0.0.1", 4425);

        try {
            this.client.send(new TestPacket("Hello, World!", System.currentTimeMillis()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread.sleep(5000L);

        this.client.close();
    }
}
