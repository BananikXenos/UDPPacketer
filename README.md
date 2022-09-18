
# UDP Packeter

Simple UDP Networking library based on Packets (like in minecraft).


## Features

- Client
- Server
- Packets
- Connections management
- Listeners
- Server and Client Builder
- Round Trip Time meter
- Time Out
- Included [example](src/example/)


## Roadmap

- Compression
- Encryption
- Better connection handling


## Donate

- My discord: Synse#3191
- [PayPal](https://www.paypal.com/paypalme/scgxenos) Donate would be cool :)
## Screenshots

![Screenshot](https://i.ibb.co/nfbCFGs/image.png)


## Usage/Examples

- Server and Client
```java
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

```

- Packet protocol
```java
package example;

import xyz.synse.udppacketer.common.packets.PacketProtocol;

public class TestProtocol extends PacketProtocol {
    public TestProtocol(){
        register(1, TestPacket.class);
    }
}
```

- Packet
```java
package example;

import xyz.synse.udppacketer.common.packets.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TestPacket extends Packet {
    private long time;

    public TestPacket(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(time);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        this.time = in.readLong();
    }

    @Override
    public String toString() {
        return "TestPacket{" +
                "time=" + time +
                '}';
    }
}
```

- Listener (works on client and server)
```java
package example;

import xyz.synse.udppacketer.common.Connection;
import xyz.synse.udppacketer.common.IListener;
import xyz.synse.udppacketer.common.packets.Packet;

public class ClientListener implements IListener {
    @Override
    public void connected(Connection connection) {
        System.out.println("[CLIENT] Connected to server");
    }

    @Override
    public void disconnected(Connection connection, String reason) {
        System.out.println("[CLIENT] Disconnected from server. Reason: " + reason);
    }

    @Override
    public void received(Packet packet, Connection connection) {
        System.out.println("[CLIENT] Received " + packet.toString());
    }

    @Override
    public void sent(Packet packet, Connection connection) {
        System.out.println("[CLIENT] Sent " + packet.toString());
    }
}
```

## License

[MIT](https://choosealicense.com/licenses/mit/)

