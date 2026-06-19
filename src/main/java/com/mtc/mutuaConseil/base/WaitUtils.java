package com.mtc.mutuaConseil.base;

public final class WaitUtils {
    public static void sleepMs(long ms){
        try { Thread.sleep(ms); } catch (InterruptedException e){ Thread.currentThread().interrupt(); }
    }
    public static void humanDelay(){
        sleepMs(1500 + (long)(Math.random()*800));
    }
    public static void humanTypeDelay(){
        sleepMs(50 + (long)(Math.random()*120));
    }
}
