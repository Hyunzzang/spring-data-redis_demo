package com.example.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class RedissonSpringDataConfig {

//    @Bean
//    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
//        return new RedissonConnectionFactory(redisson);
//    }

//    @Bean(destroyMethod="shutdown")
//    RedissonClient redissonClient() throws IOException {
//        Config config = new Config();
//        config.useClusterServers()
//                .addNodeAddress("redis://localhost:6379");
//        return Redisson.create(config);
//    }
}
