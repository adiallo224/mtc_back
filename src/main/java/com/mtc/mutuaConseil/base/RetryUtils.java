package com.mtc.mutuaConseil.base;

import java.util.function.Supplier;

public final class RetryUtils {
    public static <T> T retry(Supplier<T> attempt, int maxAttempts, long waitMs) {
        int tries = 0;
        while(true){
            tries++;
            try {
                return attempt.get();
            } catch (Exception e) {
                if(tries >= maxAttempts) throw e;
                WaitUtils.sleepMs(waitMs);
            }
        }
    }
}
