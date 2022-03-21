package com.example.redis.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
// todo: Jackson2 사용시 (spring -> objcet) 기본생성자 있어야 함.
@NoArgsConstructor
public class User implements Serializable {
    private String name;
    private String phone;

    @Builder
    public User(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}
