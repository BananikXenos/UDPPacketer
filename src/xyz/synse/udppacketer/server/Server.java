package xyz.synse.udppacketer.server;

import org.jetbrains.annotations.Nullable;
import xyz.synse.udppacketer.PacketerException;
import xyz.synse.udppacketer.common.Connection;
import xyz.synse.udppacketer.common.IListener;
import xyz.synse.udppacketer.common.packets.Packet;
import xyz.synse.udppacketer.common.packets.PacketProtocol;
import xyz.synse.udppacketer.common.utils.Constants;
import xyz.synse.udppacketer.common.utils.Framework;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Server {
    private final int serverPort;
    private DatagramSocket datagramSocket;
    private final ArrayList<Connection> connections = new ArrayList<>();
    private final ArrayList<IListener> listeners = new ArrayList<>();
    private final PacketProtocol packetProtocol;

    public Server(int port, PacketProtocol packetProtocol) {
        this.serverPort = port;
        this.packetProtocol = packetProtocol;
    }

    public void start(){
        if(this.datagramSocket == null || this.datagramSocket.isClosed()){
            try {
                this.datagramSocket = new DatagramSocket(this.serverPort);
            } catch (SocketException e) {
                throw new PacketerException("Failed to initialize socket", e);
            }

            startReceiveThread();
            startTimeOutThread();
        }
    }

    private void startReceiveThread() {
        new Thread(() -> {
            while (!datagramSocket.isClosed()) {
                try {
                    byte[] receiveBuf = new byte[Constants.RECEIVE_BUFFER_SIZE];
                    DatagramPacket datagramPacket = new DatagramPacket(receiveBuf, receiveBuf.length);

                    datagramSocket.receive(datagramPacket);

                    InetAddress address = datagramPacket.getAddress();
                    int port = datagramPacket.getPort();
                    DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(datagramPacket.getData()));
                    int packetType = inStream.readInt();

                    if (packetType == Framework.CONNECT) {
                        Connection existing = getConnection(address, port);
                        if (existing != null) {
                            sendConnected(new Connection(address, port));
                            System.out.println("Already connected client " + existing);
                            continue;
                        }

                        Connection connection = new Connection(address, port);
                        connections.add(connection);

                        sendConnected(connection);

                        for (IListener listener : listeners)
                            listener.connected(connection);
                        continue;
                    }

                    if (packetType == Framework.DISCONNECT) {
                        Connection connection = getConnection(address, port);
                        if (connection == null) {
                            sendDisconnected(new Connection(address, port));
                            System.out.println("Already disconnected client address=" + address.toString() + ", port=" + port);
                            continue;
                        }

                        connections.remove(connection);
                        sendDisconnected(connection);

                        for (IListener listener : listeners)
                            listener.disconnected(connection, "Disconnected");
                        continue;
                    }

                    if (packetType == Framework.RTT_REQUEST) {
                        Connection connection = getConnection(address, port);
                        if (connection == null) {
                            sendDisconnected(new Connection(address, port));
                            System.out.println("Client is disconnected (address=" + address.toString() + ", port=" + port + ") but is sending RTT request.");
                            continue;
                        }

                        connection.getTimeOutTimer().reset();
                        sendRTTAnswer(inStream.readLong(), connection);
                        continue;
                    }

                    if (packetType == Framework.PACKET) {
                        Connection connection = getConnection(address, port);
                        if (connection == null) {
                            sendDisconnected(new Connection(address, port));
                            System.out.println("Client is disconnected (address=" + address.toString() + ", port=" + port + ") but is sending packets.");
                            continue;
                        }

                        Packet packet = packetProtocol.createClientboundPacket(inStream.readInt(), inStream);

                        for (IListener listener : listeners)
                            listener.received(packet, connection);
                        continue;
                    }
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                         NoSuchMethodException e) {
                    throw new PacketerException("Error while receiving data", e);
                } catch (IOException ex){}
            }
        }).start();
    }

    private void startTimeOutThread(){
        new Thread(() -> {
            while(!datagramSocket.isClosed()){
                if(connections.isEmpty())
                    continue;

                Iterator<Connection> i = connections.iterator();
                while (i.hasNext()) {
                    try {
                        Connection con = i.next();
                        if (con.getTimeOutTimer().hasElapsed(Constants.TIMEOUT, true)) {
                            sendDisconnected(con);

                            for (IListener listener : listeners)
                                listener.disconnected(con, "Timed out");

                            i.remove();
                        }
                    }catch (NoSuchElementException | ConcurrentModificationException ignored){}
                }
            }
        }).start();
    }

    public void addListener(IListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IListener listener) {
        listeners.remove(listener);
    }

    public void send(Packet packet, Connection connection) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataOutStream = new DataOutputStream(outStream);

            dataOutStream.writeInt(Framework.PACKET);
            dataOutStream.writeInt(packetProtocol.getServerboundId(packet));
            packet.write(dataOutStream);

            byte[] bytes = outStream.toByteArray();

            dataOutStream.close();

            datagramSocket.send(new DatagramPacket(bytes, bytes.length, connection.getAddress(), connection.getPort()));

            for(IListener listener : listeners)
                listener.sent(packet, connection);
        } catch (IOException e) {
            throw new PacketerException("Failed to send packet", e);
        }
    }

    public void sendToAll(Packet packet) {
        for(Connection connection : connections) {
            try {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                DataOutputStream dataOutStream = new DataOutputStream(outStream);

                dataOutStream.writeInt(Framework.PACKET);
                dataOutStream.writeInt(packetProtocol.getServerboundId(packet));
                packet.write(dataOutStream);

                byte[] bytes = outStream.toByteArray();

                dataOutStream.close();

                datagramSocket.send(new DatagramPacket(bytes, bytes.length, connection.getAddress(), connection.getPort()));

                for(IListener listener : listeners)
                    listener.sent(packet, connection);
            } catch (IOException e) {
                throw new PacketerException("Failed to send packet", e);
            }
        }
    }

    private void sendRTTAnswer(long time, Connection connection) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataOutStream = new DataOutputStream(outStream);

            dataOutStream.writeInt(Framework.RTT_ANSWER);
            dataOutStream.writeLong(time);

            byte[] bytes = outStream.toByteArray();

            dataOutStream.close();

            datagramSocket.send(new DatagramPacket(bytes, bytes.length, connection.getAddress(), connection.getPort()));
        } catch (IOException e) {
            throw new PacketerException("Failed to send packet", e);
        }
    }

    public void kick(Connection connection){
        connections.remove(connection);
        sendDisconnected(connection);

        for (IListener listener : listeners)
            listener.disconnected(connection, "Kicked");
    }

    private void sendDisconnected(Connection connection) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataOutStream = new DataOutputStream(outStream);

            dataOutStream.writeInt(Framework.DISCONNECTED);

            byte[] bytes = outStream.toByteArray();

            dataOutStream.close();

            datagramSocket.send(new DatagramPacket(bytes, bytes.length, connection.getAddress(), connection.getPort()));
        } catch (IOException e) {
            throw new PacketerException("Failed to send packet", e);
        }
    }

    private void sendConnected(Connection connection) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataOutStream = new DataOutputStream(outStream);

            dataOutStream.writeInt(Framework.CONNECTED);

            byte[] bytes = outStream.toByteArray();

            dataOutStream.close();

            datagramSocket.send(new DatagramPacket(bytes, bytes.length, connection.getAddress(), connection.getPort()));
        } catch (IOException e) {
            throw new PacketerException("Failed to send packet", e);
        }
    }

    public void close(){
        for(Connection connection : connections){
            sendDisconnected(connection);
        }
        datagramSocket.close();
    }

    @Nullable
    public Connection getConnection(InetAddress address, int port) {
        for (Connection connection : connections) {
            if (connection.getPort() == port && connection.getAddress() == address)
                return connection;
        }

        return null;
    }

    public ArrayList<Connection> getConnections() {
        return connections;
    }
}
