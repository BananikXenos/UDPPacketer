package example;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Test {
    public static void main(String[] args) throws SocketException, UnknownHostException, InterruptedException {
        new UDPServerTest();

        Thread.sleep(3000L);

        new UDPClientTest();
    }
}
