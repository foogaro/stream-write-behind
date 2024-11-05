package com.foogaro.redis.wbs.core.listener;

import com.foogaro.redis.wbs.core.orchestrator.ProcessOrchestrator;
import com.foogaro.redis.wbs.core.processor.Processor;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.*;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;

import static com.foogaro.redis.wbs.core.Misc.*;

public abstract class AbstractStreamListener<T, R> implements StreamListener<T, R> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Class<T> entityClass;
    private final Class<R> repositoryClass;

    public abstract ProcessOrchestrator<T, R> getProcessOrchestrator();
    public abstract Processor<T, R> getProcessor();

    protected AbstractStreamListener() {
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.repositoryClass = (Class<R>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }

    protected Class<T> getEntityClass() {
        return this.entityClass;
    }

    protected Class<R> getRepositoryClass() {
        return repositoryClass;
    }

    @PostConstruct
    private void startListening() {
        logger.info("Starting to listen on stream {} for entity {} managed by repository {}", getStreamKey(getEntityClass()), getEntityClass().getSimpleName(), getRepositoryClass().getSimpleName());

        try {
            getRedisTemplate().opsForStream().createGroup(getStreamKey(getEntityClass()), ReadOffset.from("0"), getConsumerGroup(getRepositoryClass()));
            logger.info("Consumer group {} created for stream {}", getConsumerGroup(getRepositoryClass()), getStreamKey(getEntityClass()));
        } catch (Throwable e) {
            if (!e.getMessage().contains("BUSYGROUP")) {
                if (e.getMessage().contains("NOGROUP")) {
                    getRedisTemplate().opsForStream().add(StreamRecords.newRecord()
                            .in(getStreamKey(getEntityClass()))
                            .ofMap(Collections.singletonMap("init", "true")));

                    getRedisTemplate().opsForStream().createGroup(getStreamKey(getEntityClass()), ReadOffset.lastConsumed(), getConsumerGroup(getRepositoryClass()));
                    logger.info("Stream {} and consumer group {} created", getStreamKey(getEntityClass()), getConsumerGroup(getRepositoryClass()));
                } else {
                    throw e;
                }
            }
        }
        getStreamMessageListenerContainer().receive(
                Consumer.from(getConsumerGroup(getRepositoryClass()), getConsumerName(getEntityClass(), getRepositoryClass())),
                StreamOffset.create(getStreamKey(getEntityClass()), ReadOffset.lastConsumed()),
                this::onMessage
        );

        getStreamMessageListenerContainer().start();
        logger.info("Listener started for stream {} for entity {} managed by repository {}", getStreamKey(getEntityClass()), getEntityClass().getSimpleName(), getRepositoryClass().getSimpleName());
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        try {
//            getProcessOrchestrator().addProcessor(getProcessor());
            getProcessOrchestrator().orchestrate(record, getProcessor());
        } catch (Exception e) {
            logger.error("Error processing record: {}", record.getId(), e);
        }
    }

}
