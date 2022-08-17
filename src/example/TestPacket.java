package example;

import com.github.bananikxenos.udppacketer.packets.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TestPacket implements Packet {
    private String coolText;
    private long currentTime;

    public TestPacket(String coolText, long currentTime){
        this.coolText = coolText;
        this.currentTime = currentTime;
    }

    public TestPacket(DataInputStream in) throws IOException {
        this.coolText = in.readUTF();
        this.currentTime = in.readLong();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(this.coolText);
        out.writeLong(this.currentTime);
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
