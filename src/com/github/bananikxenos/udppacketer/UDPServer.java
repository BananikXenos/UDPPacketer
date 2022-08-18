package com.github.bananikxenos.udppacketer;

import com.github.bananikxenos.udppacketer.connections.Connection;
import com.github.bananikxenos.udppacketer.listener.ServerListener;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.PacketProtocol;
import com.github.bananikxenos.udppacketer.packets.headers.PacketsSendMode;
import com.github.bananikxenos.udppacketer.threads.server.ServerReceiveThread;
import com.github.bananikxenos.udppacketer.threads.server.ServerRttThread;
import com.github.bananikxenos.udppacketer.threads.server.ServerSendThread;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.CopyOnWriteArrayList;

public class UDPServer {
    /* Clients socket */
    private DatagramSocket socket;

    /* Packet Protocol */
    private PacketProtocol packetProtocol;

    /* Clients packet listener */
    private ServerListener serverListener;

    /* Threads */
    private ServerReceiveThread serverReceiveThread;
    private ServerSendThread serverSendThread;
    private ServerRttThread serverRttThread;

    /* Packet Send Mode */
    private PacketsSendMode packetsSendMode = PacketsSendMode.POLL;

    /* Buffer for packets */
    private byte[] buf = new byte[2048];

    /* Compression */
    private boolean useCompression = true;

    /* Connections */
    private CopyOnWriteArrayList<Connection> connections = new CopyOnWriteArrayList<>();

    private long clientTimeout = 15_000L;

    /**
     * Constructor of the Server
     *
     * @param packetProtocol        packet protocol
     * @param serverListener listener
     */
    public UDPServer(PacketProtocol packetProtocol, ServerListener serverListener) {
        this.packetProtocol = packetProtocol;
        this.serverListener = serverListener;
    }

    /**
     * Starts the server on port
     *
     * @param port port
     * @throws SocketException exception
     */
    public synchronized void start(int port) throws SocketException {
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

        this.serverRttThread = new ServerRttThread(this);
        this.serverRttThread.start();

        System.out.println("SERVER >> Server Started.");
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
    public ServerListener getListener() {
        return this.serverListener;
    }

    /**
     * Sets the packet listener
     *
     * @param listener Packet Listener
     */
    public void setListener(ServerListener listener) {
        this.serverListener = listener;
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

    public void send(Connection connection, Packet packet) throws IOException {
        // Sends packet with settings
        serverSendThread.addToSending(packet, connection.getAddress(), connection.getPort());
    }

    /**
     * Stops the server
     */
    public void stop() {
        this.socket.close();

        this.serverSendThread.stop();
        this.serverReceiveThread.stop();
        this.serverRttThread.stop();

        System.out.println("SERVER >> Server Closed.");
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

    public CopyOnWriteArrayList<Connection> getConnections() {
        return connections;
    }

    public ServerReceiveThread getServerReceiveThread() {
        return serverReceiveThread;
    }

    public ServerSendThread getServerSendThread() {
        return serverSendThread;
    }

    public ServerRttThread getServerRttThread() {
        return serverRttThread;
    }

    public void setClientTimeout(long clientTimeout) {
        this.clientTimeout = clientTimeout;
    }

    public long getClientTimeout() {
        return clientTimeout;
    }
}
