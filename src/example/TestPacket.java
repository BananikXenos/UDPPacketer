package example;

import com.github.bananikxenos.udppacketer.packets.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TestPacket extends Packet {
    private String coolText;
    private long currentTime;

    public TestPacket(String coolText, long currentTime) {
        this.coolText = coolText;
        this.currentTime = currentTime;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(this.coolText);
        out.writeLong(this.currentTime);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        this.coolText = in.readUTF();
        this.currentTime = in.readLong();
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public String getCoolText() {
        return coolText;
    }

    public void setCoolText(String coolText) {
        this.coolText = coolText;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }
}
