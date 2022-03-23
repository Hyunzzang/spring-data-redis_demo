package com.example.redis.service;

import com.example.redis.util.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQuantityService {

    private final RedisTemplate<String, Long> incrRedisTemplate;

    private final DistributedLock distributedLockCustom;

    private ReentrantLock reentrantLock = new ReentrantLock();
    private StampedLock stampedLock = new StampedLock();


    /**
     * 제고 감소 시키는 방법
     * not thread-safe
     *
     * @param prodId
     * @param buyQuantity
     */
    public void procBuyQuantity(String prodId, int buyQuantity) {
        ValueOperations<String, Long> valusOps = incrRedisTemplate.opsForValue();

        Long stockedQuantity = valusOps.get(makeKey(prodId));
        if (stockedQuantity <= 0) {
            throw new IllegalStateException("[제고부족]");
        }
        if ((stockedQuantity - buyQuantity) <= 0) {
            throw new IllegalStateException("[판매수량부족]");
        }
        valusOps.decrement(makeKey(prodId), buyQuantity);
    }

    /**
     * 판매된 수량을 증가 시키는 방법
     * not thread-safe
     *
     * @param prodId
     * @param totalQuantity
     * @param buyQuantity
     */
    public void procBuyIncrement(String prodId, int totalQuantity, int buyQuantity) {
        ValueOperations<String, Long> valusOps = incrRedisTemplate.opsForValue();

        Long stockedQuantity = valusOps.get(makeKey(prodId));
        log.info("{}-stockedQuantity:{}", Thread.currentThread().getName(), stockedQuantity);
        if (stockedQuantity >= totalQuantity) {
            throw new IllegalStateException("[제고부족]");
        }
        if ((stockedQuantity + buyQuantity) > totalQuantity) {
            throw new IllegalStateException("[판매수량부족]");
        }
        Long resVal = valusOps.increment(makeKey(prodId), buyQuantity);
        log.info("{}-resVal:{}", Thread.currentThread().getName(), resVal);
    }

    /**
     * 판매된 수량을 증가 시키는 방법(lock)
     * thread-safe
     *
     * @param prodId
     * @param totalQuantity
     * @param buyQuantity
     * @throws InterruptedException
     */
    public void buyIncrement_localLock(String prodId, int totalQuantity, int buyQuantity) throws InterruptedException {
        ValueOperations<String, Long> valusOps = incrRedisTemplate.opsForValue();

        boolean isLockAcquired = reentrantLock.tryLock(500, TimeUnit.MILLISECONDS);

        if (isLockAcquired) {
            try {
                Long stockedQuantity = valusOps.get(makeKey(prodId));
                log.info("{}-stockedQuantity:{}", Thread.currentThread().getName(), stockedQuantity);
                if (stockedQuantity >= totalQuantity) {
                    throw new IllegalStateException("[제고부족]");
                }
                if ((stockedQuantity + buyQuantity) > totalQuantity) {
                    throw new IllegalStateException("[판매수량부족]");
                }
                Long resVal = valusOps.increment(makeKey(prodId), buyQuantity);
                log.info("{}-resVal:{}", Thread.currentThread().getName(), resVal);
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    public void buyIncrement_stampedLock(String prodId, int totalQuantity, int buyQuantity) throws InterruptedException {
        ValueOperations<String, Long> valusOps = incrRedisTemplate.opsForValue();

//        long stamp = stampedLock.tryReadLock();
//        log.info("{}-validate:{}", Thread.currentThread().getName(), stampedLock.validate(stamp));
//        try {
//            Long stockedQuantity = valusOps.get(makeKey(prodId));
//            log.info("{}-stockedQuantity:{}", Thread.currentThread().getName(), stockedQuantity);
//            if (stockedQuantity >= totalQuantity) {
//                throw new IllegalStateException("[제고부족]");
//            }
//            if ((stockedQuantity + buyQuantity) > totalQuantity) {
//                throw new IllegalStateException("[판매수량부족]");
//            }
//        } finally {
//            if (StampedLock.isReadLockStamp(stamp))
//                stampedLock.unlock(stamp);
//        }

        long writeStamp = stampedLock.tryWriteLock(500, TimeUnit.MILLISECONDS);
        log.info("{}-writeStamp:{}", Thread.currentThread().getName(), writeStamp);
        try {
            Long stockedQuantity = valusOps.get(makeKey(prodId));
            log.info("{}-stockedQuantity:{}", Thread.currentThread().getName(), stockedQuantity);
            if (stockedQuantity >= totalQuantity) {
                throw new IllegalStateException("[제고부족]");
            }
            if ((stockedQuantity + buyQuantity) > totalQuantity) {
                throw new IllegalStateException("[판매수량부족]");
            }

            Long resVal = valusOps.increment(makeKey(prodId), buyQuantity);
            log.info("{}-resVal:{}", Thread.currentThread().getName(), resVal);
        } finally {
            stampedLock.unlockWrite(writeStamp);
        }
    }

    public void buyIncrement_distributedLock(String prodId, int totalQuantity, int buyQuantity) throws InterruptedException {
        ValueOperations<String, Long> valusOps = incrRedisTemplate.opsForValue();

        boolean isLock = distributedLockCustom.tryLock(prodId, 500, TimeUnit.MILLISECONDS);

        if (isLock) {
            try {
                Long stockedQuantity = valusOps.get(makeKey(prodId));
                log.info("{}-stockedQuantity:{}", Thread.currentThread().getName(), stockedQuantity);
                if (stockedQuantity >= totalQuantity) {
                    throw new IllegalStateException("[제고부족]");
                }
                if ((stockedQuantity + buyQuantity) > totalQuantity) {
                    throw new IllegalStateException("[판매수량부족]");
                }
                Long resVal = valusOps.increment(makeKey(prodId), buyQuantity);
                log.info("{}-resVal:{}", Thread.currentThread().getName(), resVal);
            } finally {
                distributedLockCustom.tryUnlock(prodId);
            }
        } else {
            log.warn("ProdId is lock: {}", prodId);
        }
    }

    public void procBuyIncrement_transaction(String prodId, int totalQuantity, int buyQuantity) {
        List<Object> txResults = incrRedisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.watch(makeKey(prodId));
                Long stockedQuantity = (Long) operations.opsForValue().get(makeKey(prodId));
                log.info("===> {}-stockedQuantity:{}", Thread.currentThread().getName(), stockedQuantity);
                if (stockedQuantity >= totalQuantity) {
                    throw new IllegalStateException("[제고부족]");
                }
                if ((stockedQuantity + buyQuantity) > totalQuantity) {
                    throw new IllegalStateException("[판매수량부족]");
                }
                operations.multi();
                operations.opsForValue().increment(makeKey(prodId), buyQuantity);

                return operations.exec();
            }
        });
//        log.info("Number of items added to set: {}", txResults.get(0));

        Long resVal = incrRedisTemplate.opsForValue().get(makeKey(prodId));
        log.info("{}-resVal:{}", Thread.currentThread().getName(), resVal);
    }

    private String makeKey(String prodId) {
        return String.format("%s:%s", "PRODUCT:STOCKED", prodId);
    }
}
