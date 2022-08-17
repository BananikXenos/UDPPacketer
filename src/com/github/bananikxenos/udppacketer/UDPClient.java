package com.github.bananikxenos.udppacketer;

import com.github.bananikxenos.udppacketer.listener.UDPNetworkingListener;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.PacketProtocol;

import java.io.*;
import java.net.*;

public class UDPClient {
    /* Clients socket */
    private final DatagramSocket socket;

    /* Servers address */
    private final InetAddress address;

    /* Servers port */
    private final int port;

    /* Packet Protocol */
    private PacketProtocol packetProtocol;

    /* Clients packet listener */
    private UDPNetworkingListener listener;

    /* Buffer for packets */
    private byte[] buf = new byte[1024];

    /**
     * Constructor of UDP Client
     *
     * @param ip            address of the server
     * @param port          port of the server
     * @param packetProtocol packet protocol
     * @param listener      packet listener
     * @throws SocketException      exception
     * @throws UnknownHostException exception
     */
    public UDPClient(String ip, int port, PacketProtocol packetProtocol, UDPNetworkingListener listener) throws SocketException, UnknownHostException {
        // Set values
        this.port = port;
        this.packetProtocol = packetProtocol;
        this.socket = new DatagramSocket();
        this.address = InetAddress.getByName(ip);
        this.listener = listener;

        // Packet receiver thread
        new Thread(() -> {
            // Loop the receiving
            while (true) {
                // Receive the packet
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Read bytes into Input
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(packet.getData());
                DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

                // Construct the packet
                Packet useFriendlyPacket = null;
                try {
                    useFriendlyPacket = packetProtocol.createServerboundPacket(dataInputStream.readInt(), dataInputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Offload the listener to another thread the keep the main receiving (maybe add the whole code into this one after receiving the packet IDK)
                // TODO: 17. 8. 2022 example.Test and add the whole constructing process to another thread to keep receiving packets
                Packet finalUseFriendlyPacket = useFriendlyPacket; // Copy the packet to temp variable
                new Thread(() -> {
                    // Check if listener isn't null & packet isn't null
                    if (this.listener != null && finalUseFriendlyPacket != null)
                        this.listener.onPacketReceived(finalUseFriendlyPacket, packet.getAddress(), packet.getPort()); // Execute the listener
                }).start();
            }
        }).start();
    }

    /**
     * Returns the Packet Protocol
     * @return Packet Protocol
     */
    public PacketProtocol getPacketProtocol() {
        return packetProtocol;
    }

    /**
     * Sets the packet protocol
     * @param packetProtocol New Packet Protocol
     */
    public void setPacketProtocol(PacketProtocol packetProtocol) {
        this.packetProtocol = packetProtocol;
    }

    /**
     * Returns the packet listener
     * @return Packet Listener
     */
    public UDPNetworkingListener getListener() {
        return listener;
    }

    /**
     * Sets the packet listener
     * @param listener Packet Listener
     */
    public void setListener(UDPNetworkingListener listener) {
        this.listener = listener;
    }

    /**
     * Returns the port of the server
     * @return Port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the Address of the server
     * @return Servers Address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Send data to the server in another Thread (Android wants this IDK why)
     * @param packet packet
     * @throws IOException exception
     */
    public void sendData(Packet packet) throws IOException {
        // Run on new thread
        new Thread(() -> {
            try {
                // Create output stream to write data to
                ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

                // Write the packet id
                dataOutputStream.writeInt(packetProtocol.getClientboundId(packet));

                // Write the packet data to output stream
                packet.write(dataOutputStream);

                // Gets the bytes from output stream to send
                byte[] msg = bufferedOutputStream.toByteArray();

                // Sends the data
                DatagramPacket p = new DatagramPacket(msg, msg.length, address, port);
                socket.send(p);
            } catch (IOException ignored) {}
        }).start();
    }

    /**
     * Sets the packet receiving buffer
     * @param size size of the buffer
     */
    public void setBuffer(int size) {
        this.buf = new byte[size];
    }

    /**
     * Returns receiving buffer size
     * @return size
     */
    public int getBufferSize() {
        return buf.length;
    }

    /**
     * Closes the client
     */
    public void close() {
        socket.close();
    }
}
