package com.example.redis.util;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {

    boolean tryLock(String key, long timeout, TimeUnit unit);

    void tryUnlock(String key);


    default String makeLockKey(String key) {
        return String.format("%s:%s", "PRODUCT:LOCK", key);
    }
}
