//package com.foogaro.redis.demo.listener.redis;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.foogaro.redis.demo.entity.Employer;
//import com.foogaro.redis.demo.processor.redis.RedisEmployerProcessOrchestrator;
//import com.foogaro.redis.demo.processor.redis.RedisEmployerProcessor;
//import com.foogaro.redis.demo.repository.redis.RedisEmployerRepository;
//import com.foogaro.redis.wbs.core.listener.AbstractStreamListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.connection.stream.MapRecord;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.stream.StreamMessageListenerContainer;
//import org.springframework.stereotype.Component;
//
//@Component
//public class RedisEmployerStreamListener extends AbstractStreamListener<Employer, RedisEmployerRepository> {
//
//    @Autowired
//    private RedisEmployerRepository employerRepository;
//
//    @Autowired
//    private RedisTemplate<String, String> redisTemplate;
//    @Autowired
//    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;
//    @Autowired
//    private ObjectMapper objectMapper;
//    @Autowired
//    private RedisEmployerProcessOrchestrator processOrchestrator;
//    @Autowired
//    private RedisEmployerProcessor processor;
//
//    @Override
//    public RedisTemplate<String, String> getRedisTemplate() {
//        return redisTemplate;
//    }
//
//    @Override
//    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> getStreamMessageListenerContainer() {
//        return streamMessageListenerContainer;
//    }
//
//    @Override
//    public ObjectMapper getObjectMapper() {
//        return objectMapper;
//    }
//
//    protected void deleteEntity(Object id) {
//        employerRepository.deleteById((Long) id);
//    }
//
//    protected Employer saveEntity(Employer entity) {
//        return employerRepository.save(entity);
//    }
//
//    @Override
//    public RedisEmployerProcessOrchestrator getProcessOrchestrator() {
//        return processOrchestrator;
//    }
//
//    @Override
//    public RedisEmployerProcessor getProcessor() {
//        return processor;
//    }
//}
