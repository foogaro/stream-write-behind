package com.foogaro.redis.service.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.config.Consts;
import com.foogaro.redis.entity.Employer;
import com.foogaro.redis.repository.redis.RedisEmployerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.StreamEntryID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.foogaro.redis.config.Consts.*;

@Service
public class RedisEmployerService {

    @Autowired
    private Jedis jedis;

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
            Pipeline pipeline = jedis.pipelined();
            pipeline.set(employer.getId().toString(), json);
            pipeline.xadd(EMPLOYER_STREAM_KEY, StreamEntryID.NEW_ENTRY, map);
            pipeline.sync();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteEmployer(Long id) {
        Map<String, String> map = new HashMap<>();
        map.put(EVENT_CONTENT_KEY, id.toString());
        map.put(EVENT_OPERATION_KEY, DELETE_OPERATION_KEY);
        Pipeline pipeline = jedis.pipelined();
        pipeline.del(id.toString());
        pipeline.xadd(Consts.EMPLOYER_STREAM_KEY, StreamEntryID.NEW_ENTRY, map);
        pipeline.sync();
    }

}
