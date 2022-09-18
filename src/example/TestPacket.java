package example;

import xyz.synse.udppacketer.common.packets.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TestPacket extends Packet {
    private long time;

    public TestPacket(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(time);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        this.time = in.readLong();
    }

    @Override
    public String toString() {
        return "TestPacket{" +
                "time=" + time +
                '}';
    }
}
