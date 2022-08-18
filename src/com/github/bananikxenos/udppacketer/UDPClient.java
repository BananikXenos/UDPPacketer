package com.github.bananikxenos.udppacketer;

import com.github.bananikxenos.udppacketer.listener.ClientListener;
import com.github.bananikxenos.udppacketer.listener.ServerListener;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.PacketProtocol;
import com.github.bananikxenos.udppacketer.packets.headers.PacketHeader;
import com.github.bananikxenos.udppacketer.packets.headers.PacketsSendMode;
import com.github.bananikxenos.udppacketer.threads.client.ClientReceiveThread;
import com.github.bananikxenos.udppacketer.threads.client.ClientRttThread;
import com.github.bananikxenos.udppacketer.threads.client.ClientSendThread;
import com.github.bananikxenos.udppacketer.utils.Timer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPClient {
    /* Clients socket */
    private DatagramSocket socket;

    /* Servers address */
    private InetAddress address;

    /* Servers port */
    private int port;

    /* Packet Protocol */
    private PacketProtocol packetProtocol;

    /* Clients packet listener */
    private ClientListener listener;

    /* Threads */
    private ClientReceiveThread clientReceiveThread;
    private ClientSendThread clientSendThread;
    private ClientRttThread clientRttThread;

    /* Packet Send Mode */
    private PacketsSendMode packetsSendMode = PacketsSendMode.POLL;

    /* Buffer for packets */
    private byte[] buf = new byte[2048];

    private boolean connected = false;

    private double SmoothRTT = 400;

    /* Compression */
    private boolean useCompression = true;

    private Timer RttSendTimer = new Timer();
    private Timer DisconnectTimer = new Timer();

    private long DisconnectTime = 15_000L;

    /**
     * Constructor of UDP Client
     *
     * @param packetProtocol packet protocol
     * @param listener       packet listener
     */
    public UDPClient(PacketProtocol packetProtocol, ClientListener listener) {
        // Set values
        this.packetProtocol = packetProtocol;
        this.listener = listener;
    }

    /**
     * Connects to server with ip and port
     * @param ip ip address
     * @param port port
     * @throws SocketException indicate that there is an error creating or accessing a Socket
     * @throws UnknownHostException indicate that the IP address of a host could not be determined
     */
    public synchronized void connect(String ip, int port) throws IOException {
        if(this.socket != null && !this.socket.isClosed())
            close();

        this.port = port;
        this.address = InetAddress.getByName(ip);

        this.socket = new DatagramSocket();

        // Packet receive thread
        this.clientReceiveThread = new ClientReceiveThread(this);
        this.clientReceiveThread.start();
        // Packet send thread
        this.clientSendThread = new ClientSendThread(this);
        this.clientSendThread.start();

        this.clientRttThread = new ClientRttThread(this);
        this.clientRttThread.start();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        dataOutputStream.writeInt(PacketHeader.CONNECT.ordinal());

        this.clientSendThread.sendPacket(byteArrayOutputStream.toByteArray());
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
        return useCompression;
    }

    /**
     * Returns the Packet Protocol
     *
     * @return Packet Protocol
     */
    public PacketProtocol getPacketProtocol() {
        return packetProtocol;
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
     * Returns the packet listener
     *
     * @return Packet Listener
     */
    public ClientListener getListener() {
        return listener;
    }

    /**
     * Sets the packet listener
     *
     * @param listener Packet Listener
     */
    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    /**
     * Returns the port of the server
     *
     * @return Port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the Address of the server
     *
     * @return Servers Address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Send data to the server in another Thread (Android wants this IDK why)
     *
     * @param packet packet
     * @throws IOException exception
     */
    public void send(Packet packet) throws IOException {
        // Sends packet with settings
        this.clientSendThread.addToSending(packet);
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
        return buf.length;
    }

    /**
     * Closes the client
     */
    public void close() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        dataOutputStream.writeInt(PacketHeader.DISCONNECT.ordinal());

        this.clientSendThread.sendPacket(byteArrayOutputStream.toByteArray());

        if (getListener() != null)
            getListener().onDisconnected(); // Execute the listener

        socket.close();

        this.clientSendThread.stop();
        this.clientReceiveThread.stop();
        this.clientRttThread.stop();
    }

    /**
     * Returns byte buffer for receiving
     * @return buffer for receiving
     */
    public byte[] getBuffer() {
        return this.buf;
    }

    /**
     * Returns client socket
     * @return client socket
     */
    public DatagramSocket getSocket() {
        return socket;
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

    public ClientReceiveThread getClientReceiveThread() {
        return clientReceiveThread;
    }

    public ClientSendThread getClientSendThread() {
        return clientSendThread;
    }

    public boolean isConnected() {
        return connected;
    }

    @Deprecated
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public double getRTT() {
        return SmoothRTT;
    }

    public void setSmoothRTT(double value) {
        this.SmoothRTT = this.SmoothRTT * 0.7 + value * 0.3;
    }

    public Timer getRttSendTimer() {
        return RttSendTimer;
    }

    public ClientRttThread getClientRttThread() {
        return clientRttThread;
    }

    public Timer getDisconnectTimer() {
        return DisconnectTimer;
    }

    public void setDisconnectTime(long disconnectTime) {
        DisconnectTime = disconnectTime;
    }

    public long getDisconnectTime() {
        return DisconnectTime;
    }
}
