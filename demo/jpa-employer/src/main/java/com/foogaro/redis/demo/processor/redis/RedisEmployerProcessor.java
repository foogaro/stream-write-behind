package com.foogaro.redis.demo.processor.redis;

import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.repository.redis.RedisEmployerRepository;
import com.foogaro.redis.wbs.core.processor.AbstractProcessor;
import com.foogaro.redis.wbs.core.service.RepositoryFinder;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedisEmployerProcessor extends AbstractProcessor<Employer, RedisEmployerRepository> {

    @Autowired
    private ListableBeanFactory beanFactory;

    private RepositoryFinder repositoryFinder;

    @Override
    public RepositoryFinder getRepositoryFinder() {
        if (repositoryFinder == null) {
             repositoryFinder = new RepositoryFinder(beanFactory);
        }
        return repositoryFinder;
    }


}
