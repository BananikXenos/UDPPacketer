package com.github.bananikxenos.udppacketer.connections;

import com.github.bananikxenos.udppacketer.utils.Timer;

import java.net.InetAddress;

public class Connection {
    private final InetAddress address;
    private final int port;

    private double SmoothRTT = 400;

    private Timer DisconnectTimer = new Timer();

    private final Timer RttSendTimer = new Timer();

    public Connection(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public double getRTT() {
        return SmoothRTT;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void setSmoothRTT(double value) {
        this.SmoothRTT = this.SmoothRTT * 0.7 + value * 0.3;
    }

    public Timer getDisconnectTimer() {
        return DisconnectTimer;
    }

    public Timer getRttSendTimer() {
        return RttSendTimer;
    }
}
