package com.github.bananikxenos.udppacketer;

import com.github.bananikxenos.udppacketer.connections.Connection;
import com.github.bananikxenos.udppacketer.listener.ServerListener;
import com.github.bananikxenos.udppacketer.packets.Packet;
import com.github.bananikxenos.udppacketer.packets.PacketProtocol;
import com.github.bananikxenos.udppacketer.packets.headers.PacketHeader;
import com.github.bananikxenos.udppacketer.packets.headers.PacketsSendMode;
import com.github.bananikxenos.udppacketer.utils.Compression;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class UDPServer {
    /* Clients socket */
    private DatagramSocket socket;

    /* Packet Protocol */
    private PacketProtocol packetProtocol;

    /* Clients packet listener */
    private final ArrayList<ServerListener> serverListeners = new ArrayList<>();

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
    private final CopyOnWriteArrayList<Connection> connections = new CopyOnWriteArrayList<>();

    /* Disconnect Time */
    private long clientTimeout = 15_000L;

    /**
     * Constructor of the Server
     *
     * @param packetProtocol packet protocol
     */
    public UDPServer(PacketProtocol packetProtocol) {
        this.packetProtocol = packetProtocol;
    }

    /**
     * Starts the server on port
     *
     * @param port port
     * @throws SocketException exception
     */
    public synchronized void start(int port) throws SocketException {
        if (this.socket != null && !this.socket.isClosed())
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

    /**
     * Returns clients socket
     * @return clients socket
     */
    public DatagramSocket getSocket() {
        return this.socket;
    }

    /**
     * Returns the packet listener
     *
     * @return Packet Listener
     */
    public ArrayList<ServerListener> getListeners() {
        return this.serverListeners;
    }

    /**
     * Adds packet listener
     *
     * @param listener Packet Listener
     */
    public void addListener(ServerListener listener) {
        this.serverListeners.add(listener);
    }

    /**
     * Removed packet listener
     *
     * @param listener Packet Listener
     */
    public void removeListener(ServerListener listener) {
        this.serverListeners.remove(listener);
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

    /**
     * Send packet to connection
     * @param connection connection
     * @param packet packet
     * @throws IOException I/O exception
     */
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
     *
     * @return buffer for receiving
     */
    public byte[] getBuffer() {
        return this.buf;
    }

    /**
     * Sets mode for sending packets
     *
     * @param packetsSendMode mode for sending packets
     */
    public void setPacketsSendMode(PacketsSendMode packetsSendMode) {
        this.packetsSendMode = packetsSendMode;
    }

    /**
     * Returns mode for sending packets
     *
     * @return mode for sending packets
     */
    public PacketsSendMode getPacketsSendMode() {
        return packetsSendMode;
    }

    /**
     * Returns Connections
     * @return Connections
     */
    public CopyOnWriteArrayList<Connection> getConnections() {
        return connections;
    }

    /**
     * Returns Server Receive Thread
     * @return Server Receive Thread
     */
    public ServerReceiveThread getServerReceiveThread() {
        return serverReceiveThread;
    }

    /**
     * Returns Server Send Thread
     * @return Server Send Thread
     */
    public ServerSendThread getServerSendThread() {
        return serverSendThread;
    }

    /**
     * Returns Server Timeout & Rtt Thread
     * @return Server Timeout & Rtt Thread
     */
    public ServerRttThread getServerRttThread() {
        return serverRttThread;
    }

    /**
     * Sets the time before client is timed out
     * @param clientTimeout time before client is timed out
     */
    public void setClientTimeout(long clientTimeout) {
        this.clientTimeout = clientTimeout;
    }

    /**
     * Returns the time before client is timed out
     * @return time before client is timed out
     */
    public long getClientTimeout() {
        return clientTimeout;
    }

    private class ServerReceiveThread extends Thread {
        private final UDPServer udpServer;

        ServerReceiveThread(UDPServer udpServer) {
            this.udpServer = udpServer;
        }

        @Override
        public void run() {
            while (udpServer != null && udpServer.getSocket() != null && !udpServer.getSocket().isClosed()) {
                // Receive the packet
                DatagramPacket packet = new DatagramPacket(udpServer.getBuffer(), udpServer.getBufferSize());
                try {
                    udpServer.getSocket().receive(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

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

                        Optional<Connection> optional = udpServer.getConnections().stream().filter(con -> con.getAddress() == packet.getAddress() && con.getPort() == packet.getPort()).findFirst();
                        Connection connection = optional.orElse(null);

                        if (packetHeader == PacketHeader.CONNECT) {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                            dataOutputStream.writeInt(PacketHeader.CONNECTED.ordinal());

                            this.udpServer.getServerSendThread().sendPacket(byteArrayOutputStream.toByteArray(), packet.getAddress(), packet.getPort());

                            if (optional.isEmpty()) {
                                udpServer.getConnections().add(connection = new Connection(packet.getAddress(), packet.getPort()));

                                // Check if listener isn't null & packet isn't null
                                for (ServerListener listener : udpServer.getListeners())
                                    listener.onClientConnected(connection); // Execute the listener
                            } else {
                                System.out.println("SERVER > Client tried to connect but he is already connected.");
                            }
                        } else if (packetHeader == PacketHeader.PACKET) {
                            if (optional.isEmpty()) {
                                System.out.println("SERVER > Received packet but client not connected.");
                                return;
                            }

                            // Construct the packet
                            Packet pPacket = udpServer.getPacketProtocol().createClientboundPacket(dataInputStream.readInt(), dataInputStream);

                            // Check if listener isn't null & packet isn't null
                            if (pPacket != null)
                                for (ServerListener listener : udpServer.getListeners())
                                    listener.onPacketReceived(pPacket, connection); // Execute the listener
                        } else if (packetHeader == PacketHeader.RTT_REPLY) {
                            if (optional.isEmpty()) {
                                System.out.println("SERVER > Received ping but client not connected.");
                                return;
                            }

                            double ping = System.currentTimeMillis() - dataInputStream.readLong();
                            connection.setSmoothRTT(ping);
                            connection.getDisconnectTimer().reset();
                        } else if (packetHeader == PacketHeader.RTT_REQUEST) {
                            if (optional.isEmpty()) {
                                System.out.println("SERVER > Received ping request but client not connected.");
                                return;
                            }

                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                            dataOutputStream.writeInt(PacketHeader.RTT_REPLY.ordinal());

                            dataOutputStream.writeLong(dataInputStream.readLong());

                            this.udpServer.getServerSendThread().sendPacket(byteArrayOutputStream.toByteArray(), packet.getAddress(), packet.getPort());
                        } else if (packetHeader == PacketHeader.DISCONNECT) {
                            if (optional.isEmpty()) {
                                System.out.println("SERVER > Received disconnect but client isn't already connected.");
                                return;
                            }

                            udpServer.getConnections().remove(connection);

                            for (ServerListener listener : udpServer.getListeners())
                                listener.onClientDisconnect(connection); // Execute the listener
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
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
            }
        }
    }

    private class ServerRttThread extends Thread {
        private final UDPServer udpServer;

        private ServerRttThread(UDPServer udpServer) {
            this.udpServer = udpServer;
        }

        @Override
        public void run() {
            while (udpServer != null && udpServer.getSocket() != null && !udpServer.getSocket().isClosed()) {
                for (Connection connection : udpServer.getConnections()) {
                    if (connection.getRttSendTimer().hasElapsed(5_000L, true)) {
                        try {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                            dataOutputStream.writeInt(PacketHeader.RTT_REQUEST.ordinal());
                            dataOutputStream.writeLong(System.currentTimeMillis());

                            udpServer.getServerSendThread().sendPacket(byteArrayOutputStream.toByteArray(), connection.getAddress(), connection.getPort());
                        } catch (Exception ignored) {
                        }
                    }

                    if (connection.getDisconnectTimer().hasElapsed(udpServer.getClientTimeout(), false)) {
                        try {
                            udpServer.getConnections().remove(connection);

                            for (ServerListener listener : udpServer.getListeners())
                                listener.onClientDisconnect(connection); // Execute the listener

                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

                            dataOutputStream.writeInt(PacketHeader.DISCONNECT.ordinal());

                            udpServer.getServerSendThread().sendPacket(byteArrayOutputStream.toByteArray(), connection.getAddress(), connection.getPort());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    private class ServerSendThread extends Thread {
        private final UDPServer udpServer;

        private final ArrayList<PendingPacket> pendingPackets = new ArrayList<>();

        public ServerSendThread(UDPServer udpServer) {
            this.udpServer = udpServer;
        }

        @Override
        public void run() {
            while (udpServer != null && udpServer.getSocket() != null && !udpServer.getSocket().isClosed()) {
                if (this.udpServer.getPacketsSendMode() == PacketsSendMode.POLL && !this.pendingPackets.isEmpty()) {
                    sendPacket(pendingPackets.remove(0));
                }
            }
        }

        private void sendPacketNewThread(PendingPacket pendingPacket) {
            new Thread(() -> sendPacket(pendingPacket)).start();
        }

        private void sendPacket(PendingPacket pendingPacket) {
            try {
                // Create output stream to write data to
                ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream(udpServer.getBufferSize());
                DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

                dataOutputStream.writeInt(PacketHeader.PACKET.ordinal());

                // Write the packet id
                dataOutputStream.writeInt(udpServer.getPacketProtocol().getClientboundId(pendingPacket.packet()));

                // Write the packet data to output stream
                pendingPacket.packet().write(dataOutputStream);

                // Gets the bytes from output stream to send
                byte[] msg = bufferedOutputStream.toByteArray();

                //Check if to use Compression
                if (udpServer.isCompression()) {
                    // Compress
                    msg = Compression.compress(msg);
                }

                // Sends the data
                DatagramPacket p = new DatagramPacket(msg, msg.length, pendingPacket.address(), pendingPacket.port());
                udpServer.getSocket().send(p);
            } catch (IOException ignored) {
            }
        }

        public void sendPacket(byte[] data, InetAddress address, int port) {
            try {

                // Gets the bytes from output stream to send
                byte[] msg = data;

                //Check if to use Compression
                if (udpServer.isCompression()) {
                    // Compress
                    msg = Compression.compress(msg);
                }

                // Sends the data
                DatagramPacket p = new DatagramPacket(msg, msg.length, address, port);
                udpServer.getSocket().send(p);
            } catch (IOException ignored) {
            }
        }

        public void addToSending(Packet packet, InetAddress address, int port) {
            if (this.udpServer.getPacketsSendMode() == PacketsSendMode.POLL)
                pendingPackets.add(new PendingPacket(packet, address, port));
            else
                sendPacketNewThread(new PendingPacket(packet, address, port));
        }

        record PendingPacket(Packet packet, InetAddress address, int port) {
        }
    }
}
