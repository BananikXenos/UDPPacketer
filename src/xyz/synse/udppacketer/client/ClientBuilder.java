package xyz.synse.udppacketer.client;

import xyz.synse.udppacketer.PacketerException;
import xyz.synse.udppacketer.common.IListener;
import xyz.synse.udppacketer.common.packets.PacketProtocol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientBuilder {
    private InetAddress address;
    private int port = 4425;
    private PacketProtocol packetProtocol;
    private ArrayList<IListener> listeners = new ArrayList<>();

    public ClientBuilder() {
        try {
            address = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ignored) {
        }
    }

    public ClientBuilder withAddress(String address) {
        try {
            this.address = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new PacketerException("Failed to set address " + address, e);
        }
        return this;
    }

    public ClientBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    public ClientBuilder withProtocol(PacketProtocol packetProtocol){
        this.packetProtocol = packetProtocol;
        return this;
    }


    public ClientBuilder withListener(IListener listener){
        this.listeners.add(listener);
        return this;
    }

    public Client build(){
        if(address == null){
            throw new PacketerException("Address not set!");
        }

        if(packetProtocol == null){
            throw new PacketerException("Protocol not set!");
        }

        Client client = new Client(address, port, packetProtocol);
        for(IListener listener : listeners){
            client.addListener(listener);
        }

        return client;
    }
}
