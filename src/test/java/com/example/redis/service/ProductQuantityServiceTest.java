package com.example.redis.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class ProductQuantityServiceTest {

    @Autowired
    private ProductQuantityService productQuantityService;

    @Resource(name = "incrRedisTemplate")
    private ValueOperations<String, Long> valusOps;

    @Autowired
    private RedisTemplate<String, Long> incrRedisTemplate;

    @BeforeEach
    public void init() {
        /**
         * 1101 : 현 재고량 저장(재고감소)
         * 1102 : 판매량 저장 (판매수량증가)
         */
        valusOps.increment("PRODUCT:STOCKED:11001", 10000);
        valusOps.increment("PRODUCT:STOCKED:11002", 0);
    }

    @AfterEach
    public void finish() {
        incrRedisTemplate.delete("PRODUCT:STOCKED:11001");
        incrRedisTemplate.delete("PRODUCT:STOCKED:11002");
    }

    @Test
    public void procBuyQuantity_shortTest() {
        productQuantityService.procBuyQuantity("11001", 10);

        Long res = valusOps.get("PRODUCT:STOCKED:11001");

        assertThat(res).isEqualTo(9990);
    }

    @Test
    public void procBuyQuantity_shortTest_판매수량부족() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> productQuantityService.procBuyQuantity("11001", 10001));

        assertThat(exception.getMessage()).isEqualTo("[판매수량부족]");
    }

    @Test
    public void procBuyIncrement_shortTest() {
        productQuantityService.procBuyIncrement("11002", 10000, 10);

        Long res = valusOps.get("PRODUCT:STOCKED:11002");

        assertThat(res).isEqualTo(10);
    }

    @Test
    public void procBuyIncrement_shortTest_판매수량부족() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> productQuantityService.procBuyIncrement("11002", 10, 11));

        assertThat(exception.getMessage()).isEqualTo("[판매수량부족]");
    }

    @Test
    public void procBuyIncrement_multiTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(30);
        IntStream.range(0, 100).forEach(i -> {
            executor.execute(() ->
                    productQuantityService.procBuyIncrement("11002", 1000, 10));
        });

        Thread.sleep(1000);

        Long res = valusOps.get("PRODUCT:STOCKED:11002");
        assertThat(res).isEqualTo(1000);
    }

    @Test
    public void procBuyIncrement_multiLockTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        IntStream.range(0, 100).forEach(i -> {
            executor.execute(() ->
            {
                try {
                    productQuantityService.buyIncrement_localLock("11002", 1000, 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });

        Thread.sleep(1000);
        Long res = valusOps.get("PRODUCT:STOCKED:11002");
        assertThat(res).isEqualTo(1000);
    }

    @Test
    public void buyIncrement_stampedLockTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        IntStream.range(0, 100).forEach(i -> {
            executor.execute(() ->
            {
                try {
                    productQuantityService.buyIncrement_stampedLock("11002", 1000, 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });

        Thread.sleep(1000);
        Long res = valusOps.get("PRODUCT:STOCKED:11002");
        assertThat(res).isEqualTo(1000);
    }

    @Test
    public void buyIncrement_distributedLockTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        IntStream.range(0, 100).forEach(i -> {
            executor.execute(() ->
            {
                try {
                    productQuantityService.buyIncrement_distributedLock("11002", 1000, 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });

        Thread.sleep(1000);
        Long res = valusOps.get("PRODUCT:STOCKED:11002");
        assertThat(res).isEqualTo(1000);
    }

    @Test
    public void procBuyIncrement_transactionTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        IntStream.range(0, 2).forEach(i -> {
            executor.execute(() -> productQuantityService.procBuyIncrement_transaction("11002", 1000, 10));
        });

        Thread.sleep(1000);
        Long res = valusOps.get("PRODUCT:STOCKED:11002");
        assertThat(res).isEqualTo(1000);
    }
}
