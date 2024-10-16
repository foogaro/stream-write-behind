package com.foogaro.redis.repository.redis;

import com.foogaro.redis.entity.Employer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisEmployerRepository extends CrudRepository<Employer, Long> {

    Employer findByEmail(String email);

}