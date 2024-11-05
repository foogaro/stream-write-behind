package com.foogaro.redis.wbs.core.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.wbs.core.orchestrator.ProcessOrchestrator;
import com.foogaro.redis.wbs.core.processor.Processor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

public interface StreamListener<T, R> {

    void onMessage(MapRecord<String, String, String> message);

    RedisTemplate<String, String> getRedisTemplate();
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> getStreamMessageListenerContainer();
    ObjectMapper getObjectMapper();
    ProcessOrchestrator<T, R> getProcessOrchestrator();
    Processor<T, R> getProcessor();

}
