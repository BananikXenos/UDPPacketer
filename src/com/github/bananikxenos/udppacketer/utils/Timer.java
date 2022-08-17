package com.github.bananikxenos.udppacketer.utils;

public class Timer {
    /* Last time reset */
    private long lastTime = System.currentTimeMillis();

    /**
     * Reset the time
     */
    public void reset(){
        this.lastTime = System.currentTimeMillis();
    }

    /**
     * Checks if timer has Elapsed
     * @param millis time elapsed
     * @param reset reset
     * @return has elapsed
     */
    public boolean hasElapsed(long millis, boolean reset){
        if((System.currentTimeMillis() - lastTime) > millis){
            if(reset)
                reset();

            return true;
        }

        return false;
    }

    /**
     * Checks if timer has Elapsed
     * @param seconds time elapsed in seconds
     * @param reset reset
     * @return has elapsed
     */
    public boolean hasElapsed(double seconds, boolean reset){
        return hasElapsed((long)(seconds * 1000L), reset);
    }
}
