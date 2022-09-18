package xyz.synse.udppacketer.common.utils;

public class Timer {
    private long lastTime = System.currentTimeMillis();

    public boolean hasElapsed(long millis, boolean reset){
        if((System.currentTimeMillis() - lastTime) >= millis){
            if(reset)
                reset();

            return true;
        }

        return false;
    }

    public void reset(){
        this.lastTime = System.currentTimeMillis();
    }
}
