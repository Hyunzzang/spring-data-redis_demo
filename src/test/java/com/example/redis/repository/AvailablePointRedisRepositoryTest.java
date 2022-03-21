package com.example.redis.repository;

import com.example.redis.model.AvailablePoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;

//@DataRedisTest
@SpringBootTest
public class AvailablePointRedisRepositoryTest {

    @Autowired
    private AvailablePointRedisRepository availablePointRedisRepository;

    @Test
    public void saveAndFindTest() {
        String randomId = createId();
        LocalDateTime now = LocalDateTime.now();

        AvailablePoint availablePoint = AvailablePoint.builder()
                .id(randomId)
                .point(1L)
                .refreshTime(now)
                .build();

        availablePointRedisRepository.save(availablePoint);

        AvailablePoint resAvailablePoint = availablePointRedisRepository.findById(randomId).get();

        assertThat(resAvailablePoint).isNotNull();
        assertThat(resAvailablePoint.getPoint()).isEqualTo(1L);
    }


    private String createId() {
        SplittableRandom random = new SplittableRandom();
        return String.valueOf(random.nextInt(1, 1_000_000_000));
    }

    /**
     * @Indexed 어노테이션이 설정된 필드는 repository 에서 조회 조건으로 사용 할 수 있음.
     * (여러 필드를 and, or 조건으로 사용도 가능함)
     */
    @Test
    public void indexedTest() {
        String randomId = createId();
        LocalDateTime now = LocalDateTime.now();

        AvailablePoint availablePoint = AvailablePoint.builder()
                .id(randomId)
                .userName("test01")
                .point(1000L)
                .refreshTime(now)
                .build();

        availablePointRedisRepository.save(availablePoint);

        AvailablePoint resAvailablePoint = availablePointRedisRepository.findByUserName("test01").get().get(0);
        assertThat(resAvailablePoint).isNotNull();
        assertThat(resAvailablePoint.getPoint()).isEqualTo(1000L);

        availablePointRedisRepository.delete(availablePoint);
    }
}
