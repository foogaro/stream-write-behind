package com.foogaro.redis.repository.jpa;

import com.foogaro.redis.entity.Employer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEmployerRepository extends JpaRepository<Employer, Long> {

    Employer findByEmail(String email);

}