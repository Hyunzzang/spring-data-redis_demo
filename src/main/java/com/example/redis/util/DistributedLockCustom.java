package com.example.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class DistributedLockCustom implements DistributedLock {

    private final StringRedisTemplate stringRedisTemplate;


    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.opsForValue().setIfAbsent(makeLockKey(key), "true", 1, TimeUnit.SECONDS);
    }

    @Override
    public void tryUnlock(String key) {
        stringRedisTemplate.delete(makeLockKey(key));
    }

}
