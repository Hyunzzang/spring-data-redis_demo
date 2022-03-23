package com.example.redis.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
    @DisplayName("단건 제고감소 테스트")
    public void procBuyQuantity_shortTest() {
        productQuantityService.procBuyQuantity("11001", 10);

        Long res = valusOps.get("PRODUCT:STOCKED:11001");

        assertThat(res).isEqualTo(9990);
    }

    @Test
    @DisplayName("단건 제고감소 판매수량부족")
    public void procBuyQuantity_shortTest_판매수량부족() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> productQuantityService.procBuyQuantity("11001", 10001));

        assertThat(exception.getMessage()).isEqualTo("[판매수량부족]");
    }

    @Test
    @DisplayName("단건 set구조 제고처리 테스트")
    public void procBuyStock_set_shortTest() {
        productQuantityService.procBuyStock("OR0001", "11002", 10000, 10);

        Long res = stringRedisTemplate.opsForSet().size("PRODUCT:SET:STOCKED:11002");
        stringRedisTemplate.opsForSet().remove("PRODUCT:SET:STOCKED:11002", "OR0001");

        assertThat(res).isEqualTo(1);
    }

    @Test
    @DisplayName("단건 제고증가 테스트")
    public void procBuyIncrement_shortTest() {
        productQuantityService.procBuyIncrement("11002", 10000, 10);

        Long res = valusOps.get("PRODUCT:STOCKED:11002");

        assertThat(res).isEqualTo(10);
    }

    @Test
    @DisplayName("단건 제고증가 판매수량부족")
    public void procBuyIncrement_shortTest_판매수량부족() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> productQuantityService.procBuyIncrement("11002", 10, 11));

        assertThat(exception.getMessage()).isEqualTo("[판매수량부족]");
    }

    @Test
    @DisplayName("대량건의 제고증가 테스트")
    public void procBuyIncrement_multiTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(30);
        IntStream.range(0, 200).forEach(i -> {
            executor.execute(() ->
                    productQuantityService.procBuyIncrement("11002", 1000, 10));
        });

        Thread.sleep(1000);

        Long res = valusOps.get("PRODUCT:STOCKED:11002");
        assertThat(res).isEqualTo(1000);
    }

    @Test
    @DisplayName("대량건의 제고증가 락 처리 테스트")
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
    @DisplayName("대량건의 제고증가 stampedLock 처리 테스트")
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
    @DisplayName("대량건의 제고증가 분산락 처리 테스트")
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
}
