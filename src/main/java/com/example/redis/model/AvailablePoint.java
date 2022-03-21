package com.example.redis.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;

@ToString
@Getter
@RedisHash("test:point:available")
public class AvailablePoint implements Serializable {
    @Id
    private String id;

    @Indexed
    private String userName;

    private Long point;
    private LocalDateTime refreshTime;

    @Builder
    public AvailablePoint(String id, String userName, Long point, LocalDateTime refreshTime) {
        this.id = id;
        this.userName = userName;
        this.point = point;
        this.refreshTime = refreshTime;
    }
}
