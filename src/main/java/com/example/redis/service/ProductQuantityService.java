package com.example.redis.service;

import com.example.redis.util.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
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
    private final StringRedisTemplate stringRedisTemplate;

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
        Long resVal = valusOps.decrement(makeKey(prodId), buyQuantity);
        log.info("{}-resVal:{}", Thread.currentThread().getName(), resVal);

        if (0 > resVal) {
            throw new IllegalStateException("[판매수량 오버]");
        }
    }

    /**
     * String or hash 자료구조에 Increments the mumber
     * 판매된 수량을 증가 시키는 방법
     * 장점: 비교적 간단 (increment 후 적합성 검증)
     * 단점: 중복 증가 위험이 있음
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

        if (totalQuantity < resVal) {
            throw new IllegalStateException("[판매수량 오버]");
        }
    }

    /**
     * set 자료구조에 상품별로 주문번호 저장
     * 장점: 중복 증가 위험이 없음(주문번호가 유일하기에)
     * 단점: 한주문에 복수건 주문시, 적합성 검증(?)
     *
     * @param orderId
     * @param prodId
     * @param totalQuantity
     * @param buyQuantity
     */
    public void procBuyStock(String orderId, String prodId, int totalQuantity, int buyQuantity) {
        SetOperations<String, String> setOps = stringRedisTemplate.opsForSet();

        Long stockedQuantity = setOps.size(makeSetKey(prodId));
        log.info("{}-stockedQuantity:{}", Thread.currentThread().getName(), stockedQuantity);
        if (stockedQuantity >= totalQuantity) {
            throw new IllegalStateException("[제고부족]");
        }
        if ((stockedQuantity + buyQuantity) > totalQuantity) {
            throw new IllegalStateException("[판매수량부족]");
        }
        Long addVal = setOps.add(makeSetKey(prodId), orderId);
        log.info("{}-addVal:{}", Thread.currentThread().getName(), addVal);

        Long lastStock = setOps.size(makeSetKey(prodId));
        log.info("{}-lastStock:{}", Thread.currentThread().getName(), lastStock);
        if (totalQuantity < lastStock) {
            throw new IllegalStateException("[판매수량 오버]");
        }
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

    /**
     * 레디스에서의 트랙잭션 처리
     *
     * @param prodId
     * @param totalQuantity
     * @param buyQuantity
     */
    public void procBuyIncrement_transaction(String prodId, int totalQuantity, int buyQuantity) {
        List<Object> txResults = incrRedisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                // watch(읽기)안에 작업이 들어 갈 경우 스레드 테스트 결과 느리고 optimistic lock 발생
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

    private String makeSetKey(String prodId) {
        return String.format("%s:%s", "PRODUCT:SET:STOCKED", prodId);
    }
}
