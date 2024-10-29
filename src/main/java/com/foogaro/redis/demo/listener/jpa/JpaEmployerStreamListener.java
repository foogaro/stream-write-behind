package com.foogaro.redis.demo.listener.jpa;

import com.foogaro.redis.core.listener.AbstractStreamListener;
import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.repository.jpa.JpaEmployerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployerStreamListener extends AbstractStreamListener<Employer, JpaEmployerRepository> {

    @Autowired
    private JpaEmployerRepository employerRepository;

    @Override
    protected JpaEmployerRepository getRepository() {
        return employerRepository;
    }

    @Override
    protected void deleteEntity(Object id) {
        employerRepository.deleteById((Long) id);
    }

    @Override
    protected Employer saveEntity(Employer entity) {
        return employerRepository.save(entity);
    }

}
