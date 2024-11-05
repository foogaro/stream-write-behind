package com.foogaro.redis.demo.processor.jpa;

import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.repository.jpa.JpaEmployerRepository;
import com.foogaro.redis.wbs.core.orchestrator.AbstractProcessOrchestrator;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployerProcessOrchestrator extends AbstractProcessOrchestrator<Employer, JpaEmployerRepository> {
}
