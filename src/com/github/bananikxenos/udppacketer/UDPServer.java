package com.github.bananikxenos.udppacketer;

import com.github.bananikxenos.udppacketer.listener.UDPNetworkingListener;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.PacketProtocol;
import com.github.bananikxenos.udppacketer.packets.sending.PacketsSendMode;
import com.github.bananikxenos.udppacketer.threads.server.ServerReceiveThread;
import com.github.bananikxenos.udppacketer.threads.server.ServerSendThread;

import java.io.IOException;
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

    /* Threads */
    private ServerReceiveThread serverReceiveThread;
    private ServerSendThread serverSendThread;

    /* Packet Send Mode */
    private PacketsSendMode packetsSendMode = PacketsSendMode.POLL;

    /* Buffer for packets */
    private byte[] buf = new byte[2048];

    /* Compression */
    private boolean useCompression = true;

    /**
     * Constructor of the Server
     *
     * @param packetProtocol        packet protocol
     * @param udpNetworkingListener listener
     */
    public UDPServer(PacketProtocol packetProtocol, UDPNetworkingListener udpNetworkingListener) {
        this.packetProtocol = packetProtocol;
        this.udpNetworkingListener = udpNetworkingListener;
    }

    /**
     * Starts the server on port
     *
     * @param port port
     * @throws SocketException exception
     */
    public void start(int port) throws SocketException {
        if(this.socket != null && !this.socket.isClosed())
            stop();

        // Set values
        this.socket = new DatagramSocket(port);

        // Packet receive thread
        this.serverReceiveThread = new ServerReceiveThread(this);
        this.serverReceiveThread.start();
        // Packet send thread
        this.serverSendThread = new ServerSendThread(this);
        this.serverSendThread.start();
    }

    /**
     * Use compression
     *
     * @param useCompression use compression
     */
    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    /**
     * Returns if compression is used
     *
     * @return use compression
     */
    public boolean isCompression() {
        return this.useCompression;
    }

    public DatagramSocket getSocket() {
        return this.socket;
    }

    /**
     * Returns the packet listener
     *
     * @return Packet Listener
     */
    public UDPNetworkingListener getListener() {
        return this.udpNetworkingListener;
    }

    /**
     * Sets the packet listener
     *
     * @param listener Packet Listener
     */
    public void setListener(UDPNetworkingListener listener) {
        this.udpNetworkingListener = listener;
    }

    /**
     * Returns the Packet Protocol
     *
     * @return Packet Protocol
     */
    public PacketProtocol getPacketProtocol() {
        return this.packetProtocol;
    }

    /**
     * Sets the packet protocol
     *
     * @param packetProtocol New Packet Protocol
     */
    public void setPacketProtocol(PacketProtocol packetProtocol) {
        this.packetProtocol = packetProtocol;
    }

    /**
     * Returns the port of the server
     *
     * @return Port
     */
    public int getPort() {
        return this.socket.getPort();
    }

    public void send(InetAddress address, int port, Packet packet) throws IOException {
        // Sends packet with settings
        serverSendThread.addToSending(packet, address, port);
    }

    /**
     * Stops the server
     */
    public void stop() {
        this.socket.close();

        this.serverSendThread.stop();
        this.serverReceiveThread.stop();
    }

    /**
     * Sets the packet receiving buffer
     *
     * @param size size of the buffer
     */
    public void setBuffer(int size) {
        this.buf = new byte[size];
    }

    /**
     * Returns receiving buffer size
     *
     * @return size
     */
    public int getBufferSize() {
        return this.buf.length;
    }

    /**
     * Returns byte buffer for receiving
     * @return buffer for receiving
     */
    public byte[] getBuffer() {
        return this.buf;
    }

    /**
     * Sets mode for sending packets
     * @param packetsSendMode mode for sending packets
     */
    public void setPacketsSendMode(PacketsSendMode packetsSendMode) {
        this.packetsSendMode = packetsSendMode;
    }

    /**
     * Returns mode for sending packets
     * @return mode for sending packets
     */
    public PacketsSendMode getPacketsSendMode() {
        return packetsSendMode;
    }
}
