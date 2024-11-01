package com.foogaro.redis.demo.service.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.wbs.core.Misc;
import com.foogaro.redis.wbs.core.service.EntityService;
import com.foogaro.redis.demo.entity.Employer;
import com.foogaro.redis.demo.repository.redis.RedisEmployerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.foogaro.redis.wbs.core.Misc.EVENT_CONTENT_KEY;
import static com.foogaro.redis.wbs.core.Misc.EVENT_OPERATION_KEY;

@Service
public class RedisEmployerService extends EntityService<Employer> {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisEmployerRepository repository;

    private ObjectMapper objectMapper = new ObjectMapper();

    public Iterable<Employer> findAll() {
        return repository.findAll();
    }

    public Optional<Employer> findById(Long id) {
        return repository.findById(id);
    }

    public Employer findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public void saveEmployer(Employer employer) {
        try {
            String json = objectMapper.writeValueAsString(employer);
            Map<String, String> map = new HashMap<>();
            map.put(EVENT_CONTENT_KEY, json);
            redisTemplate.opsForStream().add(getStreamKey(), map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteEmployer(Long id) {
        Map<String, String> map = new HashMap<>();
        map.put(EVENT_CONTENT_KEY, id.toString());
        map.put(EVENT_OPERATION_KEY, Misc.Operation.DELETE.getValue());
        redisTemplate.opsForStream().add(getStreamKey(), map);
    }
}
