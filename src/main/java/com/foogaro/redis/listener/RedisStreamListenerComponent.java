package com.foogaro.redis.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.config.Consts;
import com.foogaro.redis.entity.Employer;
import com.foogaro.redis.service.EmployerService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class RedisStreamListenerComponent {

    @Autowired
    private EmployerService employerService;

    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;
    //StreamListener<String, MapRecord<String, String, String>> streamListener;
    public RedisStreamListenerComponent(StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer) {
//                                        StreamListener<String, MapRecord<String, String, String>> streamListener) {
        this.streamMessageListenerContainer = streamMessageListenerContainer;
        //this.streamListener = streamListener;
    }

    @PostConstruct
    public void startListening() {
        StreamListener<String, MapRecord<String, String, String>> streamListener = new StreamListener<>() {
            @Override
            public void onMessage(MapRecord<String, String, String> message) {
                System.out.println("Received message: " + message.getValue());
                String stream = message.getStream();
                System.out.println("Stream: " + stream);
                Map map = message.getValue();
                System.out.println("Message: " + map);
                map.entrySet().stream().forEach(o -> System.out.println("in the map ------------------>: " + o));
                String json = (String) map.get("json");
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    Employer employer = objectMapper.readValue(json, Employer.class);
                    System.out.println("Redis Employer: " + employer);
                    employer = employerService.saveEmployer(employer);
                    System.out.println("Jpa Employer: " + employer);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

            }
        };

        Subscription subscription = streamMessageListenerContainer.receive(StreamOffset.latest(Consts.STREAM_KEY),
                streamListener);

        streamMessageListenerContainer.start();
    }
}