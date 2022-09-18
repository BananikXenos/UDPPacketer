package xyz.synse.udppacketer.server;

import xyz.synse.udppacketer.PacketerException;
import xyz.synse.udppacketer.common.IListener;
import xyz.synse.udppacketer.common.packets.PacketProtocol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ServerBuilder {
    private int port = 4425;
    private PacketProtocol packetProtocol;
    private ArrayList<IListener> listeners = new ArrayList<>();

    public ServerBuilder withPort(int port) {
        this.port = port;
        return this;
    }
    
    public ServerBuilder withProtocol(PacketProtocol packetProtocol){
        this.packetProtocol = packetProtocol;
        return this;
    }

    public ServerBuilder withListener(IListener listener){
        this.listeners.add(listener);
        return this;
    }
    
    public Server build(){
        if(packetProtocol == null){
            throw new PacketerException("Protocol not set!");
        }
        
        Server server = new Server(port, packetProtocol);
        for(IListener listener : listeners){
            server.addListener(listener);
        }

        return server;
    }
}
