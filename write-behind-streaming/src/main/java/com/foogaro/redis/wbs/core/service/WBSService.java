package com.foogaro.redis.wbs.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.wbs.core.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.foogaro.redis.wbs.core.Misc.EVENT_CONTENT_KEY;
import static com.foogaro.redis.wbs.core.Misc.EVENT_OPERATION_KEY;

public abstract class WBSService<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Class<T> entityClass;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public WBSService() {
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.streamKey = Misc.getStreamKey(entityClass);
    }

    //    private final WBSProperties properties;
    private final String streamKey;

    public String save(T entity) {
        try {
            String json = objectMapper.writeValueAsString(entity);
            Map<String, String> map = new HashMap<>();
            map.put(EVENT_CONTENT_KEY, json);
            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .withId(RecordId.autoGenerate())
                    .ofMap(map)
                    .withStreamKey(streamKey);
            String recordId = save(record);
            logger.debug("RecordId {} added for ingestion to the Stream {}", recordId, streamKey);
            return recordId;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String save(MapRecord<String, String, String> mapRecord) {
        RecordId recordId = redisTemplate.opsForStream().add(mapRecord);
        return Objects.nonNull(recordId) ? recordId.getValue() : null;
    }

    public void delete(Object id) {
        Map<String, String> map = new HashMap<>();
        map.put(EVENT_CONTENT_KEY, id.toString());
        map.put(EVENT_OPERATION_KEY, Misc.Operation.DELETE.getValue());
        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .withId(RecordId.autoGenerate())
                .ofMap(map)
                .withStreamKey(streamKey);
        RecordId recordId = redisTemplate.opsForStream().add(record);
        logger.debug("RecordId {} added for deletion to the Stream {}", Objects.nonNull(recordId) ? recordId.getValue() : "<null>", streamKey);
    }

}
