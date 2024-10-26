package com.foogaro.redis.listener.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.config.Consts;
import com.foogaro.redis.entity.Employer;
import com.foogaro.redis.service.redis.RedisEmployerService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RedisEmployerStreamListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RedisEmployerService redisEmployerService;

    @Autowired
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;

    @PostConstruct
    public void startListening() {
        StreamListener<String, MapRecord<String, String, String>> streamListener = new StreamListener<>() {
            @Override
            public void onMessage(MapRecord<String, String, String> message) {
                logger.info("Received message: {}", message.getValue());
                String stream = message.getStream();
                logger.info("Stream: {}", stream);
                Map map = message.getValue();
                logger.info("Message: {}", map);
                String content = (String) map.get(Consts.EVENT_CONTENT_KEY);
                logger.info("Content: {}", content);
                String operation = (String) map.get(Consts.EVENT_OPERATION_KEY);
                logger.info("Operation: {}", operation);
                if (operation != null) {
                    if (operation.equalsIgnoreCase(Consts.DELETE_OPERATION_KEY)) {
                        redisEmployerService.deleteEmployer(Long.valueOf(content));
                    }
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        Employer employer = objectMapper.readValue(content, Employer.class);
                        logger.info("Redis Employer: {}", employer);
                        redisEmployerService.saveEmployer(employer);
                        logger.info("Jpa Employer: {}", employer);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        Subscription subscription = streamMessageListenerContainer.receive(StreamOffset.latest(Consts.EMPLOYER_STREAM_KEY),
                streamListener);

        streamMessageListenerContainer.start();
    }
}