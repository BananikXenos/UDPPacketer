package example;

import com.github.bananikxenos.udppacketer.UDPServer;
import com.github.bananikxenos.udppacketer.connections.Connection;
import com.github.bananikxenos.udppacketer.listener.ServerListener;
import com.github.bananikxenos.udppacketer.packets.Packet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPServerTest {
    private UDPServer server;

    public UDPServerTest() throws SocketException {
        this.server = new UDPServer(new ExampleProtocol());
        this.server.addListener(new ServerListener() {

            @Override
            public void onClientConnected(Connection connection) {
                System.out.println("SERVER >> Client connected " + connection.getAddress() + ":" + connection.getPort());
            }

            @Override
            public void onClientDisconnect(Connection connection) {
                System.out.println("SERVER >> Client Disconnected " + connection.getAddress() + ":" + connection.getPort());
            }

            @Override
            public void onPacketReceived(Packet packet, Connection connection) {
                if(packet instanceof TestPacket testPacket) {
                    System.out.println("SERVER >> Received example.Test Packet With coolString: " + testPacket.getCoolText() + " and Time: " + testPacket.getCurrentTime());

                    try {
                        server.send(connection, new TestPacket("Hello Back!", System.currentTimeMillis()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        this.server.start(4425);
    }
}
