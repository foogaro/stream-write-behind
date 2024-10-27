package com.foogaro.redis.demo.listener.redis;

import com.foogaro.redis.core.listener.AbstractStreamListener;
import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.repository.redis.RedisEmployerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedisEmployerStreamListener extends AbstractStreamListener<Employer, RedisEmployerRepository> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RedisEmployerRepository employerRepository;

    @Override
    protected RedisEmployerRepository getRepository() {
        return employerRepository;
    }

    @Override
    protected void deleteEntity(Long id) {
        employerRepository.deleteById(id);
    }

    @Override
    protected Employer saveEntity(Employer entity) {
        return employerRepository.save(entity);
    }

}
