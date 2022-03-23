//package com.example.redis.util;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RedissonClient;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@RequiredArgsConstructor
//@Component
//public class DistributedLockRedisson implements DistributedLock{
//
//    private final RedissonClient redisson;
//
//    @Override
//    public boolean tryLock(String key, long timeout, TimeUnit unit) {
//        try {
//            return redisson.getLock(makeLockKey(key)).tryLock(timeout, unit);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        return Boolean.FALSE;
//    }
//
//    @Override
//    public void tryUnlock(String key) {
//        redisson.getLock(makeLockKey(key)).unlock();
//    }
//}
