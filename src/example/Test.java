package example;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        new UDPServerTest();

        Thread.sleep(3000L);

        new UDPClientTest();
    }
}
