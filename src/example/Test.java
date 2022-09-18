package example;

import xyz.synse.udppacketer.client.Client;
import xyz.synse.udppacketer.client.ClientBuilder;
import xyz.synse.udppacketer.server.Server;
import xyz.synse.udppacketer.server.ServerBuilder;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        Server server = new ServerBuilder().withPort(22566).withProtocol(new TestProtocol()).withListener(new ServerListener()).build();
        server.start();

        Client client = new ClientBuilder().withAddress("127.0.0.1").withPort(22566).withProtocol(new TestProtocol()).withListener(new ClientListener()).build();
        client.connect();

        client.send(new TestPacket(System.currentTimeMillis()));

        Client client2 = new ClientBuilder().withAddress("127.0.0.1").withPort(22566).withProtocol(new TestProtocol()).withListener(new ClientListener()).build();
        client2.connect();

        client2.send(new TestPacket(5000L));

        Thread.sleep(5000);

        server.kick(server.getConnections().get(0));

        server.close();
    }
}
