package com.foogaro.redis.demo.repository.redis;

import com.foogaro.redis.demo.entity.Employer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisEmployerRepository extends CrudRepository<Employer, Long> {

    Employer findByEmail(String email);

}