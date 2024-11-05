package com.foogaro.redis.demo.processor.redis;

import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.repository.redis.RedisEmployerRepository;
import com.foogaro.redis.wbs.core.orchestrator.AbstractProcessOrchestrator;
import org.springframework.stereotype.Component;

@Component
public class RedisEmployerProcessOrchestrator extends AbstractProcessOrchestrator<Employer, RedisEmployerRepository> {
}
