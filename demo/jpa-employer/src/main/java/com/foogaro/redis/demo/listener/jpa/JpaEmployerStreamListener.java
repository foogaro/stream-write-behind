package com.foogaro.redis.demo.listener.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.repository.jpa.JpaEmployerRepository;
import com.foogaro.redis.wbs.core.listener.AbstractStreamListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
public class JpaEmployerStreamListener extends AbstractStreamListener<Employer, JpaEmployerRepository> {

    @Autowired
    private JpaEmployerRepository employerRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public RedisTemplate<String, String> getRedisTemplate() {
        return redisTemplate;
    }

    @Override
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> getStreamMessageListenerContainer() {
        return streamMessageListenerContainer;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
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
