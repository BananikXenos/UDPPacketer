package xyz.synse.udppacketer.common;

import xyz.synse.udppacketer.common.utils.Timer;

import java.net.InetAddress;

public class Connection {
    private final InetAddress address;
    private final int port;
    private final Timer timeOutTimer = new Timer();
    private double SmoothRTT = 400L;

    public Connection(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public double getSmoothRTT() {
        return SmoothRTT;
    }

    public void setSmoothRTT(double value){
        this.SmoothRTT = this.SmoothRTT * 0.7 + value * 0.3;
    }

    public Timer getTimeOutTimer() {
        return timeOutTimer;
    }

    @Override
    public String toString() {
        return "address=" + address + ", port=" + port;
    }
}
