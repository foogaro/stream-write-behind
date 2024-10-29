package com.foogaro.redis.core.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.redis.core.Misc;
import com.foogaro.redis.core.exception.AcknowledgeMessageException;
import com.foogaro.redis.core.exception.ProcessMessageException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.foogaro.redis.core.Misc.*;

public abstract class AbstractStreamListener<T, R> implements StreamListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${wb.stream.listener.pel.attempts:3}")
    protected int MAX_RETRY_ATTEMPTS;
    @Value("${wb.stream.listener.pel.timeout:10000}")
    protected long PENDING_MESSAGE_TIMEOUT;
    @Value("${wb.stream.listener.pel.size:50}")
    protected int BATCH_SIZE;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    protected StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;

    @Autowired
    protected ObjectMapper objectMapper;

    private final Class<T> entityClass;
    private final Class<R> repositoryClass;

    protected abstract R getRepository();
    protected abstract void deleteEntity(Object id);
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

                    redisTemplate.opsForStream().createGroup(getStreamKey(), ReadOffset.lastConsumed(), getConsumerGroup());
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

        streamMessageListenerContainer.start();
        logger.info("Listener started for stream {} for entity {} managed by repository {}", getStreamKey(), getEntityClass().getSimpleName(), getRepositoryClass().getSimpleName());
    }

    private void dumpMessage(MapRecord<String, String, String> message) {
        try {
            logger.debug("Stream ID: {}", message.getStream());
            logger.debug("Message ID: {}", message.getId());
            logger.debug("Message.Content: {}", message.getValue().get(EVENT_CONTENT_KEY));
            logger.debug("Message.Operation: {}", message.getValue().get(EVENT_OPERATION_KEY));
        } catch (Exception e) {
            logger.error("Error dumping message: {}", message, e);
        }
    }

    public void onMessage(MapRecord<String, String, String> message) {
        dumpMessage(message);
        logger.info("Receiving message with ID {} from Stream {}.", message.getId(), message.getStream());
        try {
            processMessage(message);
            acknowledgeMessage(message);
        } catch (ProcessMessageException | AcknowledgeMessageException e) {
            logger.error("Error receiving message ID {} for Stream {} - {}", message.getId(), message.getStream(), message.getValue());
            logger.error("Error: ", e.getMessage());
        }
    }

    private void processMessage(MapRecord<String, String, String> message) throws ProcessMessageException {
        logger.debug("Processing message: {}", message.getId());
        String operation = message.getValue().get(EVENT_OPERATION_KEY);
        if (Misc.Operation.DELETE.getValue().equalsIgnoreCase(operation)) {
            deleteEntity(message.getValue().get(EVENT_CONTENT_KEY));
        } else {
            try {
                T entity = objectMapper.readValue(message.getValue().get(EVENT_CONTENT_KEY), getEntityClass());
                logger.debug("Entity from Stream: {}", entity);
                entity = saveEntity(entity);
                logger.debug("Entity {} saved {} by repository {}", getEntityClass(), entity, AopUtils.getTargetClass(getRepositoryClass()).getSimpleName());
            } catch (Exception e) {
                // will be picked up by the processPendingMessages method
                throw new ProcessMessageException(e);
            }
        }
        logger.info("Processed message: {}", message.getId());
    }

    private void acknowledgeMessage(MapRecord<String, String, String> message) throws AcknowledgeMessageException {
        try {
            logger.debug("Acknowledging message: {}", message.getId());
            redisTemplate.opsForStream().acknowledge(getConsumerGroup(), message);
            logger.info("Acknowledged message: {}", message.getId());
        } catch (Exception e) {
            logger.error("Error acknowledging message: {}", message.getId(), e);
            // will be picked up by the processPendingMessages method
            throw new AcknowledgeMessageException(e);
        }
    }

    @Scheduled(fixedDelay = 5000)
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
                                BATCH_SIZE);

                if (pendingMessages != null) {
                    for (PendingMessage pm : pendingMessages) {
                        String messageId = pm.getIdAsString();
                        long elapsedTime = pm.getElapsedTimeSinceLastDelivery().toMillis();
                        logger.info("Message ID {} re-processing", messageId);

                        MapRecord<String, String, String> message = null;
                            List<MapRecord<String, Object, Object>> rawMessages =
                                    redisTemplate.opsForStream().range(streamKey,
                                            Range.closed(messageId, messageId));
                        List<MapRecord<String, String, String>> messages =
                                rawMessages
                                .stream()
                                .map(this::convertMapRecord)
                                .collect(Collectors.toList());

                        if (!messages.isEmpty()) {
                            message = messages.get(0);
                            Long counter = incrementCounterKey(getCounterKey(message.getId().getValue()));
                            logger.debug("Attempts: {} - Elapsed time: {}", counter, elapsedTime);
                            if (counter > MAX_RETRY_ATTEMPTS) {
                                ProcessMessageException e = new ProcessMessageException("Too many attempts");
                                handleMessageFailure(message, e, getCounterKey(message.getId().getValue()));
                                throw new RuntimeException(e);
                            }
                            if (elapsedTime > PENDING_MESSAGE_TIMEOUT) {
                                ProcessMessageException e = new ProcessMessageException("Long lasting message");
                                handleMessageFailure(message, e, getCounterKey(message.getId().getValue()));
                                throw new RuntimeException(e);
                            }
                            try {
                                processMessage(message);
                                acknowledgeMessage(message);
                                expireCounterKey(getCounterKey(message.getId().getValue()));
                                logger.info("Successfully processed pending message: {}", messageId);
                            } catch (ProcessMessageException e) {
                                logger.error("Error processing pending message: {}", messageId, e.getMessage());
                                if (counter > MAX_RETRY_ATTEMPTS) {
                                    handleMessageFailure(message, new RuntimeException(e), getCounterKey(message.getId().getValue()));
                                    throw new RuntimeException(e);
                                }
                            } catch (AcknowledgeMessageException e) {
                                if (counter > MAX_RETRY_ATTEMPTS) {
                                    handleMessageFailure(message, new RuntimeException(e), getCounterKey(message.getId().getValue()));
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            } else {
                logger.debug("Pending messages not found for group {}", groupName);
            }
        } catch (Exception e) {
            logger.error("Error processing pending messages: {}", e.getMessage());
        }
    }

    private void handleMessageFailure(MapRecord<String, String, String> message,
                                      Exception cause,
                                      String counterKey) {
        handleDLQ(message, cause);
        expireCounterKey(counterKey);
    }

    private Boolean expireCounterKey(String counterKey) {
        return redisTemplate.expire(counterKey, Duration.ZERO);
    }

    private Long incrementCounterKey(String counterKey) {
        return redisTemplate.opsForValue().increment(counterKey);
    }

    private String getCounterKey(String id) {
        return getStreamKey()+":"+id;
    }

    private MapRecord<String, String, String> convertMapRecord(MapRecord<String, Object, Object> record) {
        Map<String, String> convertedMap = new HashMap<>();
        record.getValue().forEach((k, v) -> {
            convertedMap.put(String.valueOf(k),String.valueOf(v));
        });

        StreamRecords.RecordBuilder rb = StreamRecords.newRecord();
        return rb.withId(record.getId()).ofMap(convertedMap).withStreamKey(record.getStream());
    }

    private void handleDLQ(MapRecord<String, String, String> message, Exception e) {
        try {
            if (message != null) {
                dumpMessage(message);
                logger.error("Received error: {}", e.getMessage());
                String deadLetterKey = getDLQStreamKey(entityClass);
                Map<String, String> deadLetterMessage = new HashMap<>(message.getValue());
                deadLetterMessage.put("error", e.getMessage());
                deadLetterMessage.put("streamKey", message.getStream());
                deadLetterMessage.put("streamID", message.getId().getValue());
                deadLetterMessage.put("consumer", getConsumerName());
                deadLetterMessage.put("group", getConsumerGroup());

                redisTemplate.opsForStream().add(
                        StreamRecords.newRecord()
                                .withId(message.getId().getValue())
                                .ofMap(deadLetterMessage)
                                .withStreamKey(deadLetterKey)
                );
                logger.warn("Message {} moved to dead letter queue for manual processing.", message.getId());
                acknowledgeMessage(message);
            }
        } catch (Exception dlqError) {
            logger.error("Error moving message to dead letter queue", dlqError.getMessage());
        }
    }
}
