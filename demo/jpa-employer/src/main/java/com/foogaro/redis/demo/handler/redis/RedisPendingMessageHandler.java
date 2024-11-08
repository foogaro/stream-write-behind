package com.foogaro.redis.demo.handler.redis;

import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.processor.redis.RedisEmployerProcessor;
import com.foogaro.redis.demo.repository.redis.RedisEmployerRepository;
import com.foogaro.redis.wbs.core.handler.AbstractPendingMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedisPendingMessageHandler extends AbstractPendingMessageHandler<Employer, RedisEmployerRepository> {

    @Autowired
    private RedisEmployerProcessor processor;

    @Override
    public RedisEmployerProcessor getProcessor() {
        return processor;
    }
}
