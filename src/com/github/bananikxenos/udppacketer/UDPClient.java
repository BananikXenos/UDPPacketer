package com.github.bananikxenos.udppacketer;

import com.github.bananikxenos.udppacketer.listener.ClientListener;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.PacketProtocol;
import com.github.bananikxenos.udppacketer.packets.headers.PacketHeader;
import com.github.bananikxenos.udppacketer.packets.headers.PacketsSendMode;
import com.github.bananikxenos.udppacketer.utils.Compression;
import com.github.bananikxenos.udppacketer.utils.Timer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.ArrayList;

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
    private final ArrayList<ClientListener> listeners = new ArrayList<>();

    /* Threads */
    private ClientReceiveThread clientReceiveThread;
    private ClientSendThread clientSendThread;
    private ClientRttThread clientRttThread;

    /* Packet Send Mode */
    private PacketsSendMode packetsSendMode = PacketsSendMode.POLL;

    /* Buffer for packets */
    private byte[] buf = new byte[2048];

    /* Is Client Connected */
    private boolean connected = false;

    /* Smoothed Round Trip Time */
    private double SmoothRTT = 400;

    /* Compression */
    private boolean useCompression = true;

    /* TimeOut Timers */
    private final Timer RttSendTimer = new Timer();
    private final Timer DisconnectTimer = new Timer();

    /* Disconnect Time */
    private long TimeoutTime = 15_000L;

    /**
     * Constructor of UDP Client
     *
     * @param packetProtocol packet protocol
     */
    public UDPClient(PacketProtocol packetProtocol) {
        // Set values
        this.packetProtocol = packetProtocol;
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

        new ClientConnectThread(this).start();
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
     * Returns the packet listener list
     *
     * @return Packet Listener list
     */
    public ArrayList<ClientListener> getListeners() {
        return listeners;
    }

    /**
     * Adds packet listener
     *
     * @param listener Packet Listener
     */
    public void addListener(ClientListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes packet listener
     *
     * @param listener Packet Listener
     */
    public void removeListener(ClientListener listener) {
        this.listeners.remove(listener);
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

        this.connected = false;

        for(ClientListener listener : listeners)
            listener.onDisconnected();

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

    /**
     * Returns the thread for Receiving Packets
     * @return Thread for Receiving Packets
     */
    public ClientReceiveThread getClientReceiveThread() {
        return clientReceiveThread;
    }

    /**
     * Returns the thread for Sending Packets
     * @return Thread for Sending Packets
     */
    public ClientSendThread getClientSendThread() {
        return clientSendThread;
    }

    /**
     * Returns the thread for KeepAlive and RTT
     * @return Thread for KeepAlive and RTT
     */
    public ClientRttThread getClientRttThread() {
        return clientRttThread;
    }

    /**
     * Returns if client is connected
     * @return Connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns the Round Trip Time
     * @return Round Trip Time
     */
    public double getRTT() {
        return SmoothRTT;
    }

    /**
     * Sets the RTT Smoothly
     * @param value value
     */
    private void setSmoothRTT(double value) {
        this.SmoothRTT = this.SmoothRTT * 0.7 + value * 0.3;
    }

    /**
     * Sets the Time you get timed out
     * @param timeoutTime time in millis
     */
    public void setTimeoutTime(long timeoutTime) {
        TimeoutTime = timeoutTime;
    }

    /**
     * Returns the timeout time
     * @return timeout time
     */
    public long getTimeoutTime() {
        return TimeoutTime;
    }

    private class ClientReceiveThread extends Thread {
        private final UDPClient udpClient;

        ClientReceiveThread(UDPClient udpClient) {
            this.udpClient = udpClient;
        }

        @Override
        public void run() {
            // Loop the receiving
            while (!udpClient.getSocket().isClosed()) {
                // Receive the packet
                DatagramPacket packet = new DatagramPacket(udpClient.getBuffer(), udpClient.getBufferSize());
                try {
                    udpClient.getSocket().receive(packet);

                    new Thread(() -> {
                        try {
                            byte[] data = packet.getData();

                            // Check if compressed
                            if (Compression.isCompressed(data)) {
                                // Decompress
                                data = Compression.decompress(data);
                            }

                            // Read bytes into Input
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

                            PacketHeader packetHeader = PacketHeader.values()[dataInputStream.readInt()];

                            if(packetHeader == PacketHeader.CONNECTED){
                                udpClient.connected = true;
                                for(ClientListener listener : udpClient.getListeners())
                                    listener.onConnected();
                            } else if (packetHeader == PacketHeader.PACKET){
                                // Construct the packet
                                Packet pPacket = udpClient.getPacketProtocol().createServerboundPacket(dataInputStream.readInt(), dataInputStream);

                                // Check if listener isn't null & packet isn't null
                                if (pPacket != null)
                                    for(ClientListener listener : udpClient.getListeners())
                                        listener.onPacketReceived(pPacket);
                            } else if (packetHeader == PacketHeader.DISCONNECT) {
                                udpClient.connected = false;
                                udpClient.close();
                            } else if (packetHeader == PacketHeader.RTT_REPLY){
                                double ping = System.currentTimeMillis() - dataInputStream.readLong();
                                udpClient.setSmoothRTT(ping);
                                udpClient.DisconnectTimer.reset();
                            } else if (packetHeader == PacketHeader.RTT_REQUEST){
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                                dataOutputStream.writeInt(PacketHeader.RTT_REPLY.ordinal());

                                dataOutputStream.writeLong(dataInputStream.readLong());

                                this.udpClient.getClientSendThread().sendPacket(byteArrayOutputStream.toByteArray());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                } catch (IOException ignored) {

                }
            }
        }
    }

    private class ClientRttThread extends Thread {
        private final UDPClient udpClient;

        ClientRttThread(UDPClient udpClient) {
            this.udpClient = udpClient;
        }

        @Override
        public void run() {
            while (udpClient != null && udpClient.getSocket() != null && !udpClient.getSocket().isClosed()) {
                if (udpClient.RttSendTimer.hasElapsed(5_000L, true) && udpClient.isConnected()) {
                    try {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                        dataOutputStream.writeInt(PacketHeader.RTT_REQUEST.ordinal());
                        dataOutputStream.writeLong(System.currentTimeMillis());

                        udpClient.getClientSendThread().sendPacket(byteArrayOutputStream.toByteArray());
                    }catch (Exception ignored){}
                }

                if(udpClient.DisconnectTimer.hasElapsed(udpClient.getTimeoutTime(), false)){
                    try {
                        this.udpClient.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private class ClientSendThread extends Thread {
        private final UDPClient udpClient;

        private final ArrayList<Packet> pendingPackets = new ArrayList<>();

        ClientSendThread(UDPClient udpClient) {
            this.udpClient = udpClient;
        }

        @Override
        public void run() {
            while (udpClient != null && udpClient.getSocket() != null && !udpClient.getSocket().isClosed()) {
                if (!this.pendingPackets.isEmpty() && udpClient.isConnected()) {
                    sendPacket(pendingPackets.remove(0));
                }
            }
        }

        private void sendPacketNewThread(Packet pendingPacket) {
            new Thread(() -> sendPacket(pendingPacket)).start();
        }

        private void sendPacket(Packet packet) {
            try {
                // Create output stream to write data to
                ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream(udpClient.getBufferSize());
                DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

                dataOutputStream.writeInt(PacketHeader.PACKET.ordinal());

                // Write the packet id
                dataOutputStream.writeInt(udpClient.getPacketProtocol().getClientboundId(packet));

                // Write the packet data to output stream
                packet.write(dataOutputStream);

                // Gets the bytes from output stream to send
                byte[] msg = bufferedOutputStream.toByteArray();

                //Check if to use Compression
                if (udpClient.isCompression()) {
                    // Compress
                    msg = Compression.compress(msg);
                }

                // Sends the data
                DatagramPacket p = new DatagramPacket(msg, msg.length, udpClient.getAddress(), udpClient.getPort());
                udpClient.getSocket().send(p);
            } catch (IOException ignored) {}
        }

        public void sendPacket(byte[] data) {
            try {
                // Gets the bytes from output stream to send
                byte[] msg = data;

                //Check if to use Compression
                if (udpClient.isCompression()) {
                    // Compress
                    msg = Compression.compress(msg);
                }

                // Sends the data
                DatagramPacket p = new DatagramPacket(msg, msg.length, udpClient.getAddress(), udpClient.getPort());
                udpClient.getSocket().send(p);
            } catch (IOException ignored) {}
        }

        public void addToSending(Packet packet) {
            if (this.udpClient.getPacketsSendMode() == PacketsSendMode.POLL)
                pendingPackets.add(packet);
            else if (this.udpClient.isConnected())
                sendPacketNewThread(packet);
        }
    }

    private class ClientConnectThread extends Thread {
        private final UDPClient udpClient;

        private final Timer reconnectTimer = new Timer();

        ClientConnectThread(UDPClient udpClient) {
            this.udpClient = udpClient;
        }

        @Override
        public void run() {
            while(!udpClient.isConnected() && this.udpClient.clientSendThread.isAlive() && this.udpClient.clientRttThread.isAlive() && this.udpClient.clientReceiveThread.isAlive()) {
                if(reconnectTimer.hasElapsed(2_000L, true)) {
                    try {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                        dataOutputStream.writeInt(PacketHeader.CONNECT.ordinal());

                        this.udpClient.clientSendThread.sendPacket(byteArrayOutputStream.toByteArray());

                        System.out.println("Connecting...");
                    }catch (IOException ignored){}
                }
            }
        }
    }
}
