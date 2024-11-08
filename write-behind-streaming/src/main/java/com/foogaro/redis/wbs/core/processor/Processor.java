package com.foogaro.redis.wbs.core.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.wbs.core.exception.AcknowledgeMessageException;
import com.foogaro.redis.wbs.core.exception.ProcessMessageException;
import com.foogaro.redis.wbs.core.service.BeanFinder;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface Processor<T, R> {

    BeanFinder getRepositoryFinder();
    RedisTemplate<String, String> getRedisTemplate();
    ObjectMapper getObjectMapper();
    T convertToEntity(String content) throws JsonProcessingException;
    MapRecord<String, String, String> getRecord();
    int getPriority();
    void setPriority(int priority);
    Class<T> getEntityClass();
    Class<R> getRepositoryClass();
    List<Repository<T, ?>> getRepositories();
    void process(final MapRecord<String, String, String> record) throws ProcessMessageException;
    void acknowledge(final MapRecord<String, String, String> record) throws AcknowledgeMessageException;

}
