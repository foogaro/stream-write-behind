//package com.foogaro.redis.listener;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.foogaro.redis.entity.Employer;
//import com.foogaro.redis.service.EmployerService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.connection.stream.MapRecord;
//import org.springframework.data.redis.connection.stream.ObjectRecord;
//import org.springframework.data.redis.stream.StreamListener;
//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//
//@Service
//public class RedisStreamListener implements StreamListener<String, MapRecord<String, String, String>> {
//
//    @Autowired
//    private EmployerService employerService;
//
//    @Override
//    public void onMessage(MapRecord<String, String, String> message) {
//        String stream = message.getStream();
//        System.out.println("Stream: " + stream);
//        Map map = message.getValue();
//        System.out.println("Message: " + map);
//        map.entrySet().stream().forEach(o -> System.out.println("in the map ------------------>: " + o));
//        ObjectMapper objectMapper = new ObjectMapper();
////        try {
////            Employer employer = objectMapper.readValue(json, Employer.class);
////            System.out.println("Redis Employer: " + employer);
////            employer = employerService.saveEmployer(employer);
////            System.out.println("Jpa Employer: " + employer);
////        } catch (JsonProcessingException e) {
////            throw new RuntimeException(e);
////        }
//    }
//}