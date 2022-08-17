package com.github.bananikxenos.udppacketer;

import com.github.bananikxenos.udppacketer.listener.UDPNetworkingListener;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.PacketProtocol;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPServer {
    /* Clients socket */
    private DatagramSocket socket;

    /* Packet Protocol */
    private PacketProtocol packetProtocol;

    /* Clients packet listener */
    private UDPNetworkingListener udpNetworkingListener;

    /* Is server running */
    private boolean running = false;

    /* Buffer for packets */
    private byte[] buf = new byte[1024];

    /**
     * Constructor of the Server
     * @param packetProtocol packet protocol
     * @param udpNetworkingListener listener
     */
    public UDPServer(PacketProtocol packetProtocol, UDPNetworkingListener udpNetworkingListener){
        this.packetProtocol = packetProtocol;
        this.udpNetworkingListener = udpNetworkingListener;
    }

    /**
     * Starts the server on port
     * @param port port
     * @throws SocketException exception
     */
    public void start(int port) throws SocketException {
        // Set values
        socket = new DatagramSocket(port);
        running = true;

        // Packet receiver thread
        new Thread(() -> {
            // Loop the receiving
            while (running){
                // Receive the packet
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Read bytes into Input
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(packet.getData());
                DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

                // Construct the packet
                Packet useFriendlyPacket = null;
                try {
                    useFriendlyPacket = packetProtocol.createClientboundPacket(dataInputStream.readInt(), dataInputStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Offload the listener to another thread the keep the main receiving (maybe add the whole code into this one after receiving the packet IDK)
                // TODO: 17. 8. 2022 example.Test and add the whole constructing process to another thread to keep receiving packets
                Packet finalUseFriendlyPacket = useFriendlyPacket; // Copy the packet to temp variable
                new Thread(() -> {
                    // Check if listener isn't null & packet isn't null
                    if (this.udpNetworkingListener != null && finalUseFriendlyPacket != null)
                        this.udpNetworkingListener.onPacketReceived(finalUseFriendlyPacket, packet.getAddress(), packet.getPort()); // Execute the listener
                }).start();
            }
        }).start();
    }

    /**
     * Returns the packet listener
     * @return Packet Listener
     */
    public UDPNetworkingListener getListener() {
        return udpNetworkingListener;
    }

    /**
     * Sets the packet listener
     * @param listener Packet Listener
     */
    public void setListener(UDPNetworkingListener listener) {
        this.udpNetworkingListener = listener;
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
     * Returns the port of the server
     * @return Port
     */
    public int getPort() {
        return socket.getPort();
    }

    public void send(InetAddress address, int port, Packet packet) throws IOException {
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
     * Stops the server
     */
    public void stop() {
        running = false;
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

}
