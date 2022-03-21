package com.example.redis.repository;

import com.example.redis.model.AvailablePoint;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface AvailablePointRedisRepository extends CrudRepository<AvailablePoint, String> {

    public Optional<List<AvailablePoint>> findByUserName(String userName);
}
