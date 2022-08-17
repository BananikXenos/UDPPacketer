
# UDPPacketer

UDPPacketer is a networking library that uses UDP protocol and Packets with PacketProtocol.
Packets use DataOutputStream & DataInputStream to use as least of network resources as possible,
so its better than Serialization.


## Features

- Simple UDP Networking
- Packet Protocol
- Client/Server classes
- Listeners
- Good Performance with Threads
- Easily Editable for your needs
- Compression
- Included [example](src/example/)


## Roadmap

- Optimize performance and simplicity

- Add Reliable UDP similar to TCP

- Encryption


## Usage

First create a packet. Packets need to be read/written to in the same order!

```java
import com.github.bananikxenos.udppacketer.packets.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TestPacket implements Packet {
    private String coolText;
    private long currentTime;

    public TestPacket(String coolText, long currentTime){
        this.coolText = coolText;
        this.currentTime = currentTime;
    }

    public TestPacket(DataInputStream in) throws IOException {
        this.coolText = in.readUTF();
        this.currentTime = in.readLong();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(this.coolText);
        out.writeLong(this.currentTime);
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public String getCoolText() {
        return coolText;
    }

    public void setCoolText(String coolText) {
        this.coolText = coolText;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }
}
```

Then create custom Protocol for packets and make it extend PacketProtocol. Then register your packets like this.

```java
import com.github.bananikxenos.udppacketer.packets.PacketProtocol;

public class ExampleProtocol extends PacketProtocol {
    public ExampleProtocol(){
        register(1, TestPacket.class, TestPacket::new);
    }
}
```

Now you can create server.

```java
import com.github.bananikxenos.udppacketer.UDPServer;
import com.github.bananikxenos.udppacketer.listener.UDPNetworkingListener;
import com.github.bananikxenos.udppacketer.packets.Packet;

import java.net.InetAddress;
import java.net.SocketException;

public class UDPServerTest {
    private UDPServer server;

    public UDPServerTest() throws SocketException {
        this.server = new UDPServer(new ExampleProtocol(), new UDPNetworkingListener() {
            @Override
            public void onPacketReceived(Packet packet, InetAddress address, int port) {
                if(packet instanceof TestPacket testPacket) {
                    System.out.println("SERVER >> Received Test Packet With coolString: " + testPacket.getCoolText() + " and Time: " + testPacket.getCurrentTime());
                }

                try {
                    server.send(address, port, new TestPacket("Hello Back!", System.currentTimeMillis()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        this.server.start(4425);
    }
}
```

And how to create a client.

```java
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
        this.client = new UDPClient("127.0.0.1", 4425, new ExampleProtocol(), new UDPNetworkingListener() {
            @Override
            public void onPacketReceived(Packet packet, InetAddress address, int port) {
                if(packet instanceof TestPacket testPacket){
                    System.out.print("CLIENT >> Received Test Packet With coolString: " + testPacket.getCoolText() + " and Time: " + testPacket.getCurrentTime());
                }
            }
        });

        try {
            this.client.sendData(new TestPacket("Hello, World!", System.currentTimeMillis()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

Now to test this out i will create a new Class with main void and use both server and client to send packet we created earlier.

```java
import java.net.SocketException;
import java.net.UnknownHostException;

public class Test {
    public static void main(String[] args) throws SocketException, UnknownHostException, InterruptedException {
        new UDPServerTest();

        Thread.sleep(3000L);

        new UDPClientTest();
    }
}
```

And after running the code we get this:
![Output](https://i.ibb.co/JjfTmZF/output.png)


## Authors

- [BananikXenos](https://github.com/BananikXenos)


## Contributing

Contributions are always welcome!

My discord: Synse#3191


## License

[MIT](https://choosealicense.com/licenses/mit/)

