package com.example.redis.repository;

import com.example.redis.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
public class RedisTemplateTest {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedisTemplate<String, User> redisTemplate;

    @Autowired
    RedisTemplate<String, User> userJsonRedisTemplate;

    @Test
    @DisplayName("레디스 string 데이터 타입 string 직렬화 테스트.")
    public void stringValueTemplateTest() {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "test:keyTest";
        String val = "keyVal";

        // 추가
        valueOperations.set(key, val);
        assertThat(valueOperations.get(key)).isEqualTo(val);

        // 수정
        valueOperations.set(key, "valTest");
        assertThat(valueOperations.get(key)).isEqualTo("valTest");

        // 삭제
        stringRedisTemplate.delete(key);
        assertThat(valueOperations.get(key)).isNull();
    }

    @Test
    @DisplayName("레디스 string 데이터 타입의 객체 바이너리 테스트.")
    public void objectTemplateTest() {
        ValueOperations<String, User> valueOperations = redisTemplate.opsForValue();
        String key = "test:usertest";
        User user = User.builder()
                .name("test1")
                .phone("000-1111-3333")
                .build();

        // 추가
        valueOperations.set(key, user);
        assertThat(valueOperations.get(key).getName()).isEqualTo("test1");
        assertThat(valueOperations.get(key).getPhone()).isEqualTo("000-1111-3333");

        // 삭제
        redisTemplate.delete(key);
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    @DisplayName("레디스 string 데이터 타입의 객체 json 직렬화 테스트.")
    public void objectJsonTemplateTest() {
        String key = "test:object:json";
        User user = User.builder()
                .name("test2")
                .phone("222-3333-5555")
                .build();

        // 추가
        userJsonRedisTemplate.opsForValue().set(key, user);
        assertThat(userJsonRedisTemplate.opsForValue().get(key).getName()).isEqualTo("test2");
        assertThat(userJsonRedisTemplate.opsForValue().get(key).getPhone()).isEqualTo("222-3333-5555");

        // 수정
        user = User.builder()
                .name("test3")
                .phone("666-7777-8888")
                .build();
        userJsonRedisTemplate.opsForValue().set(key, user);
        assertThat(userJsonRedisTemplate.opsForValue().get(key).getName()).isEqualTo("test3");
        assertThat(userJsonRedisTemplate.opsForValue().get(key).getPhone()).isEqualTo("666-7777-8888");

        // 삭제
        userJsonRedisTemplate.delete(key);
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    public void expireTemplateTest() throws InterruptedException {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "test:expire";
        String val = "keyVal";

        // 추가 후 5초 후 삭제 됨
        valueOperations.set(key, val);
        stringRedisTemplate.expire(key, 5, TimeUnit.SECONDS);
//        valueOperations.getAndExpire(key, 5, TimeUnit.SECONDS);
        assertThat(valueOperations.get(key)).isEqualTo(val);

        Thread.sleep(5000L);

        // 수정
        assertThat(valueOperations.get(key)).isNull();
    }

    @Test
    public void expireByDurationTemplateTest() throws InterruptedException {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "test:expire2";
        String val = "keyVal";
        Duration timeout = Duration.ofSeconds(5);

        // 추가 후 5초 후 삭제 됨
        valueOperations.set(key, val);
        String resVal = valueOperations.getAndExpire(key, timeout);
//        stringRedisTemplate.expire(key, timeout);
        assertThat(resVal).isEqualTo(val);

        Thread.sleep(5000L);

        // 수정
        assertThat(valueOperations.get(key)).isNull();
    }
}
