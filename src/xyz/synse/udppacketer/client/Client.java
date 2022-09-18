package xyz.synse.udppacketer.client;

import xyz.synse.udppacketer.PacketerException;
import xyz.synse.udppacketer.common.Connection;
import xyz.synse.udppacketer.common.IListener;
import xyz.synse.udppacketer.common.packets.Packet;
import xyz.synse.udppacketer.common.packets.PacketProtocol;
import xyz.synse.udppacketer.common.utils.Constants;
import xyz.synse.udppacketer.common.utils.Framework;
import xyz.synse.udppacketer.common.utils.Timer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class Client {
    private DatagramSocket datagramSocket;
    private final Connection connection;
    private final ArrayList<IListener> listeners = new ArrayList<>();
    private final PacketProtocol packetProtocol;
    private boolean connected = false;
    private final Object syncObject = new Object();

    public Client(InetAddress address, int port, PacketProtocol packetProtocol) {
        this.connection = new Connection(address, port);
        this.packetProtocol = packetProtocol;
    }

    public void connect(){
        if(this.datagramSocket == null || this.datagramSocket.isClosed() || !connected){
            try {
                this.datagramSocket = new DatagramSocket();
            } catch (SocketException e) {
                throw new PacketerException("Failed to initialize socket", e);
            }

            startReceiveThread();
            startTimeOutThread();
            startConnectThread();

            synchronized(syncObject) {
                try {
                    // Calling wait() will block this thread until another thread
                    // calls notify() on the object.
                    syncObject.wait();
                } catch (InterruptedException e) {
                    // Happens if someone interrupts your thread.
                }
            }
        }
    }

    private void startReceiveThread() {
        new Thread(() -> {
            while (!datagramSocket.isClosed()) {
                try {
                    byte[] receiveBuf = new byte[Constants.RECEIVE_BUFFER_SIZE];
                    DatagramPacket datagramPacket = new DatagramPacket(receiveBuf, receiveBuf.length, connection.getAddress(), connection.getPort());

                    datagramSocket.receive(datagramPacket);

                    DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(datagramPacket.getData()));
                    int packetType = inStream.readInt();

                    // TODO: 18. 9. 2022
                    if (packetType == Framework.CONNECTED) {
                        connected = true;
                        synchronized(syncObject) {
                            syncObject.notify();
                        }
                        for (IListener listener : listeners)
                            listener.connected(connection);
                        continue;
                    }

                    // TODO: 18. 9. 2022
                    if (packetType == Framework.DISCONNECTED) {
                        connected = false;
                        for (IListener listener : listeners)
                            listener.disconnected(connection, "Disconnected");
                        datagramSocket.close();
                        continue;
                    }

                    if (packetType == Framework.RTT_REQUEST) {
                        if(!connected)
                            continue;
                        connection.getTimeOutTimer().reset();
                        sendRTTAnswer(inStream.readLong());
                        continue;
                    }

                    if (packetType == Framework.RTT_ANSWER) {
                        connection.getTimeOutTimer().reset();
                        connection.setSmoothRTT(System.currentTimeMillis() - inStream.readLong());
                        continue;
                    }

                    if (packetType == Framework.PACKET) {
                        Packet packet = packetProtocol.createServerboundPacket(inStream.readInt(), inStream);

                        for (IListener listener : listeners)
                            listener.received(packet, connection);
                        continue;
                    }
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                         NoSuchMethodException e) {
                    throw new PacketerException("Error while receiving data", e);
                }
                catch (IOException ignored){

                }
            }
        }).start();
    }

    private void startTimeOutThread() {
        new Thread(() -> {
            while (!datagramSocket.isClosed()) {
                if (connection.getTimeOutTimer().hasElapsed(Constants.TIMEOUT, true)) {
                    for (IListener listener : listeners)
                        listener.disconnected(connection, "Timed out");
                    datagramSocket.close();
                }

                if (connection.getRttTimer().hasElapsed(Constants.RTT_TIMER, true) && connected) {
                    sendRTTRequest(System.currentTimeMillis());
                }
            }
        }).start();
    }

    private void startConnectThread() {
        new Thread(() -> {
            Timer connectTimer = new Timer();
            while (!datagramSocket.isClosed() && !connected) {
                if (connectTimer.hasElapsed(Constants.RECONNECT, true)) {
                    sendConnect();
                }
            }
        }).start();
    }

    public boolean isConnected(){
        return connected;
    }

    public void addListener(IListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IListener listener) {
        listeners.remove(listener);
    }

    public void close() {
        sendDisconnect();
        datagramSocket.close();
    }

    public void send(Packet packet) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataOutStream = new DataOutputStream(outStream);

            dataOutStream.writeInt(Framework.PACKET);
            dataOutStream.writeInt(packetProtocol.getClientboundId(packet));
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

    private void sendRTTAnswer(long time) {
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

    private void sendRTTRequest(long time) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataOutStream = new DataOutputStream(outStream);

            dataOutStream.writeInt(Framework.RTT_REQUEST);
            dataOutStream.writeLong(time);

            byte[] bytes = outStream.toByteArray();

            dataOutStream.close();

            datagramSocket.send(new DatagramPacket(bytes, bytes.length, connection.getAddress(), connection.getPort()));
        } catch (IOException e) {
            throw new PacketerException("Failed to send packet", e);
        }
    }

    private void sendDisconnect() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataOutStream = new DataOutputStream(outStream);

            dataOutStream.writeInt(Framework.DISCONNECT);

            byte[] bytes = outStream.toByteArray();

            dataOutStream.close();

            datagramSocket.send(new DatagramPacket(bytes, bytes.length, connection.getAddress(), connection.getPort()));
        } catch (IOException e) {
            throw new PacketerException("Failed to send packet", e);
        }
    }

    private void sendConnect() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataOutStream = new DataOutputStream(outStream);

            dataOutStream.writeInt(Framework.CONNECT);

            byte[] bytes = outStream.toByteArray();

            dataOutStream.close();

            datagramSocket.send(new DatagramPacket(bytes, bytes.length, connection.getAddress(), connection.getPort()));
        } catch (IOException e) {
            throw new PacketerException("Failed to send packet", e);
        }
    }
}
