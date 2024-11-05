package com.foogaro.redis.demo.processor.jpa;

import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.repository.jpa.JpaEmployerRepository;
import com.foogaro.redis.wbs.core.processor.AbstractProcessor;
import com.foogaro.redis.wbs.core.service.RepositoryFinder;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployerProcessor extends AbstractProcessor<Employer, JpaEmployerRepository> {

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
