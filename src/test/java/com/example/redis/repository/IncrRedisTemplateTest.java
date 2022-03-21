package com.example.redis.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class IncrRedisTemplateTest {

    @Resource(name = "incrRedisTemplate")
    private ValueOperations<String, Long> valusOps;

    // todo: hash 타입에 저장 할수 있는 필드 개수
    // Every hash can store up to 232 - 1 field-value pairs (more than 4 billion).
    @Resource(name = "incrRedisTemplate")
    private HashOperations<String, String, Long> hashOps;

    @Test
    public void incrAndDecrStringTypeTest() {
        String key = "test:prod:count";

        // increment 테스트
        valusOps.increment(key);
        assertThat(valusOps.get(key)).isEqualTo(1);

        // param increment 테스트
        valusOps.increment(key, 5);
        assertThat(valusOps.get(key)).isEqualTo(6);

        // decrement 테스트
        valusOps.decrement(key);
        assertThat(valusOps.get(key)).isEqualTo(5);

        // param decrement 테스트
        valusOps.decrement(key, 5);
        assertThat(valusOps.get(key)).isEqualTo(0);
    }

    @Test
    public void incrAndDecrHashTypeTest() {
        String key = "test:prod:hcount";
        String hkey = "100001";

        // increment 테스트
        hashOps.increment(key, hkey, 1);
        assertThat(hashOps.get(key, hkey)).isEqualTo(1);

        // param increment 테스트
        hashOps.increment(key, hkey, 5);
        assertThat(hashOps.get(key, hkey)).isEqualTo(6);

        // decrement 테스트
        hashOps.increment(key, hkey, -1);
        assertThat(hashOps.get(key, hkey)).isEqualTo(5);

        // param decrement 테스트
        hashOps.increment(key, hkey, -5);
        assertThat(hashOps.get(key, hkey)).isEqualTo(0);
    }
}
