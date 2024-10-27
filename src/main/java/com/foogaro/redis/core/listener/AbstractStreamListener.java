package com.foogaro.redis.core.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.core.Misc;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.foogaro.redis.core.Misc.*;

public abstract class AbstractStreamListener<T, R> implements StreamListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    protected StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;

    @Autowired
    protected ObjectMapper objectMapper;

    private final Class<T> entityClass;
    private final Class<R> repositoryClass;

    protected abstract R getRepository();
    protected abstract void deleteEntity(Long id);
    protected abstract T saveEntity(T entity);

    @SuppressWarnings("unchecked")
    public AbstractStreamListener() {
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.repositoryClass = (Class<R>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }

    protected Class<T> getEntityClass() {
        return this.entityClass;
    }

    public Class<R> getRepositoryClass() {
        return repositoryClass;
    }

    protected String getStreamKey() {
        return Misc.getStreamKey(entityClass);
    }

    protected String getConsumerGroup() {
        return getRepositoryClass().getSimpleName().toLowerCase() + "_group";
    }

    protected String getConsumerName() {
        return this.entityClass.getSimpleName().toLowerCase() + "_" + getRepositoryClass().getSimpleName().toLowerCase() + "_consumer";
    }

    public void onMessage(MapRecord<String, String, String> message) {
        String messageId = message.getId().getValue();
        logger.info("Processing message with ID: {}", messageId);
        try {
            processMessage(message);
            acknowledgeMessage(message);
        } catch (Exception e) {
            logger.error("Error processing message: {}", messageId, e);
            handleMessageError(message, e);
        }
    }

    @PostConstruct
    public void startListening() {
        logger.info("Starting to listen on stream {} for entity {} managed by repository {}", getStreamKey(), getEntityClass().getSimpleName(), getRepositoryClass().getSimpleName());

        try {
            redisTemplate.opsForStream().createGroup(getStreamKey(), ReadOffset.from("0"), getConsumerGroup());
            logger.info("Consumer group {} created for stream {}", getConsumerGroup(), getStreamKey());
        } catch (Throwable e) {
            if (!e.getMessage().contains("BUSYGROUP")) {
                if (e.getMessage().contains("NOGROUP")) {
                    redisTemplate.opsForStream().add(StreamRecords.newRecord()
                            .in(getStreamKey())
                            .ofMap(Collections.singletonMap("init", "true")));

                    redisTemplate.opsForStream().createGroup(getStreamKey(), getConsumerGroup());
                    logger.info("Stream {} and consumer group {} created", getStreamKey(), getConsumerGroup());
                } else {
                    throw e;
                }
            }
        }
        streamMessageListenerContainer.receive(
                Consumer.from(getConsumerGroup(), getConsumerName()),
                StreamOffset.create(getStreamKey(), ReadOffset.lastConsumed()),
                this::onMessage
        );

//        streamMessageListenerContainer.receive(StreamOffset.latest(getStreamKey()), this::onMessage);
        streamMessageListenerContainer.start();
        logger.info("Listener started for stream {} for entity {} managed by repository {}", getStreamKey(), getEntityClass().getSimpleName(), getRepositoryClass().getSimpleName());
    }

    private void processMessage(MapRecord<String, String, String> message) throws Exception {
        logger.info("Received message: {}", message.getValue());
        String stream = message.getStream();
        logger.info("Stream: {}", stream);
        Map<String, String> map = message.getValue();
        logger.info("Message: {}", map);
        String content = map.get(EVENT_CONTENT_KEY);
        logger.info("Content: {}", content);
        String operation = map.get(EVENT_OPERATION_KEY);
        logger.info("Operation: {}", operation);
        if (operation != null && operation.equalsIgnoreCase(DELETE_OPERATION_KEY)) {
            deleteEntity(Long.valueOf(content));
        } else {
            try {
                T entity = objectMapper.readValue(content, getEntityClass());
                logger.info("Entity from Stream: {}", entity);
                entity = saveEntity(entity);
                logger.info("Entity {} saved {} by repository {}", getEntityClass(), entity, AopUtils.getTargetClass(getRepositoryClass()).getSimpleName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void acknowledgeMessage(MapRecord<String, String, String> message) {
        try {
            redisTemplate.opsForStream().acknowledge(getConsumerGroup(), message);
            logger.info("Message {} acknowledged", message.getId());
        } catch (Exception e) {
            logger.error("Error acknowledging message: {}", message.getId(), e);
            throw new RuntimeException("Failed to acknowledge message", e);
        }
    }

    private void handleMessageError(MapRecord<String, String, String> message, Exception e) {
        String messageId = message.getId().getValue();
        logger.error("Message {} processing failed", messageId, e);

        try {
            redisTemplate.opsForStream().acknowledge(getConsumerGroup(), message);
            logger.info("Failed message {} acknowledged", messageId);
        } catch (Exception ackError) {
            logger.error("Error acknowledging failed message: {}", messageId, ackError);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void processPendingMessages() {
        String streamKey = getStreamKey();
        String groupName = getConsumerGroup();
        String consumerName = getConsumerName();

        try {
            PendingMessagesSummary pendingSummary = redisTemplate.opsForStream()
                    .pending(streamKey, groupName);

            if (pendingSummary != null && pendingSummary.getTotalPendingMessages() > 0) {
                logger.info("Found {} pending messages for group {}",
                        pendingSummary.getTotalPendingMessages(), groupName);

                PendingMessages pendingMessages = redisTemplate.opsForStream()
                        .pending(streamKey,
                                Consumer.from(groupName, consumerName),
                                Range.unbounded(),
                                50);

                if (pendingMessages != null) {
                    for (PendingMessage pm : pendingMessages) {
                        String messageId = pm.getIdAsString();
                        long elapsedTime = pm.getElapsedTimeSinceLastDelivery().toMillis();

                        if (elapsedTime > 300000) {
                            List<MapRecord<String, String, String>> messages =
                                    redisTemplate.opsForStream().range(streamKey,
                                            Range.closed(messageId, messageId));

                            if (!messages.isEmpty()) {
                                MapRecord<String, String, String> message = messages.get(0);

                                try {
                                    processMessage(message);
                                    acknowledgeMessage(message);
                                    logger.info("Successfully processed pending message: {}", messageId);
                                } catch (Exception e) {
                                    logger.error("Error processing pending message: {}", messageId, e);
                                    if (pm.getTotalDeliveryCount() > 3) {
                                        handleDeadLetter(message, e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error processing pending messages", e);
        }
    }
    private void handleDeadLetter(MapRecord<String, String, String> message, Exception e) {
        try {
            String deadLetterKey = getStreamKey() + ":dead_letter";
            Map<String, String> deadLetterMessage = new HashMap<>(message.getValue());
            deadLetterMessage.put("error", e.getMessage());
            deadLetterMessage.put("timestamp", String.valueOf(System.currentTimeMillis()));

            redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(deadLetterKey)
                            .ofMap(deadLetterMessage)
            );

            acknowledgeMessage(message);

            logger.info("Message {} moved to dead letter queue", message.getId());
        } catch (Exception dlqError) {
            logger.error("Error moving message to dead letter queue", dlqError);
        }
    }
}
