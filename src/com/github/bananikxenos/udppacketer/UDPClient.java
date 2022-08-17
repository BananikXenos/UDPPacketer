package com.github.bananikxenos.udppacketer;

import com.github.bananikxenos.udppacketer.listener.UDPNetworkingListener;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.PacketProtocol;
import com.github.bananikxenos.udppacketer.utils.Compression;

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
    private byte[] buf = new byte[2048];

    /* Compression */
    private boolean useCompression = true;

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

                byte[] data = new byte[getBufferSize()];

                System.arraycopy(packet.getData(), 0, data, 0, getBufferSize());

                // Check if compressed
                if (Compression.isCompressed(data)) {
                    // Decompress
                    data = Compression.decompress(data);
                }

                // Read bytes into Input
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
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
     * Use compression
     * @param useCompression use compression
     */
    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    /**
     * Returns if compression is used
     * @return use compression
     */
    public boolean isCompression() {
        return useCompression;
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
                ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream(getBufferSize());
                DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

                // Write the packet id
                dataOutputStream.writeInt(packetProtocol.getClientboundId(packet));

                // Write the packet data to output stream
                packet.write(dataOutputStream);

                // Gets the bytes from output stream to send
                byte[] msg = bufferedOutputStream.toByteArray();

                //Check if to use Compression
                if(isCompression()){
                    // Compress
                    msg = Compression.compress(msg);
                }

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
