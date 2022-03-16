package com.example.redis.repository;

import com.example.redis.model.AvailablePoint;
import org.springframework.data.repository.CrudRepository;

public interface AvailablePointRedisRepository extends CrudRepository<AvailablePoint, String> {
}
