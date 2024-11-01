package com.foogaro.redis.wbs.core.listener;

import com.foogaro.redis.wbs.core.Misc;
import com.foogaro.redis.wbs.core.exception.AcknowledgeMessageException;
import com.foogaro.redis.wbs.core.exception.ProcessMessageException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.foogaro.redis.wbs.core.Misc.*;

/***
 * The AbstractStreamListener serves as a foundational abstract class for implementing Redis Stream listeners
 * in a robust and organized manner. It provides a comprehensive framework for consuming and processing messages from
 * Redis Streams, handling message acknowledgments, managing consumer groups, and implementing error recovery mechanisms.
 *
 * The class includes functionality for processing both new messages and pending messages (messages that failed previous
 * processing attempts), implements a Dead Letter Queue (DLQ) mechanism for handling irretrievably failed messages, and
 * maintains retry counters for failed message processing attempts.
 *
 * It automatically schedules periodic checks for pending messages, ensuring no messages are lost or stuck in processing
 * limbo.
 * @param <T> the Entity class
 * @param <R> the Repository class
 */
public abstract class AbstractStreamListener<T, R> implements StreamListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${wb.stream.listener.pel.max-attempts:3}")
    protected int MAX_ATTEMPTS;
    @Value("${wb.stream.listener.pel.max-retention:10000}")
    protected long MAX_RETENTION;
    @Value("${wb.stream.listener.pel.batch-size:50}")
    protected int BATCH_SIZE;

    private final Class<T> entityClass;
    private final Class<R> repositoryClass;

    protected abstract void deleteEntity(Object id);
    protected abstract T saveEntity(T entity);

    @SuppressWarnings("unchecked")
    public AbstractStreamListener() {
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.repositoryClass = (Class<R>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }

    /***
     * Returns the class of the entity defined by the implemented class.
     * @return Class<T> the entity class.
     */
    protected Class<T> getEntityClass() {
        return this.entityClass;
    }

    /***
     * Returns the class of the repository defined by the implemented class.
     * @return Class<T> repository class.
     */
    public Class<R> getRepositoryClass() {
        return repositoryClass;
    }

    /***
     * Returns the key of the Stream based of the entity class name.
     * @return String the generated key.
     */
    protected String getStreamKey() {
        return Misc.getStreamKey(entityClass);
    }

    /***
     * Returns the name of the consumer group, based on the repository class name.
     * @return String the generated consumer group name.
     */
    protected String getConsumerGroup() {
        return getRepositoryClass().getSimpleName().toLowerCase() + CONSUMER_GROUP_SUFFIX;
    }

    /***
     * Returns the name of the consumer, based on the entity and repository class name.
     * @return String
     */
    protected String getConsumerName() {
        return this.entityClass.getSimpleName().toLowerCase() + VALUE_SEPARATOR + getRepositoryClass().getSimpleName().toLowerCase() + CONSUMER_SUFFIX;
    }

    /***
     * The `@PostConstruct` method creates the consumer group and the consumer if they do not already exist.
     * It then begins reading the stream from the correct offset: starting from offset "0-0" if this is the first run,
     * or from the `lastConsumed` message otherwise.
     */
    @PostConstruct
    public void startListening() {
        logger.info("Starting to listen on stream {} for entity {} managed by repository {}", getStreamKey(), getEntityClass().getSimpleName(), getRepositoryClass().getSimpleName());

        try {
            getRedisTemplate().opsForStream().createGroup(getStreamKey(), ReadOffset.from("0"), getConsumerGroup());
            logger.info("Consumer group {} created for stream {}", getConsumerGroup(), getStreamKey());
        } catch (Throwable e) {
            if (!e.getMessage().contains("BUSYGROUP")) {
                if (e.getMessage().contains("NOGROUP")) {
                    getRedisTemplate().opsForStream().add(StreamRecords.newRecord()
                            .in(getStreamKey())
                            .ofMap(Collections.singletonMap("init", "true")));

                    getRedisTemplate().opsForStream().createGroup(getStreamKey(), ReadOffset.lastConsumed(), getConsumerGroup());
                    logger.info("Stream {} and consumer group {} created", getStreamKey(), getConsumerGroup());
                } else {
                    throw e;
                }
            }
        }
        getStreamMessageListenerContainer().receive(
                Consumer.from(getConsumerGroup(), getConsumerName()),
                StreamOffset.create(getStreamKey(), ReadOffset.lastConsumed()),
                this::onMessage
        );

        getStreamMessageListenerContainer().start();
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

    /***
     * This method is triggered when new messages are received from the stream; it processes the message and sends an acknowledgment if successful.
     * @param message is the message received from the stream.
     */
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

    /***
     * This method processes incoming messages by passing them to the repository's `saveEntity` method.
     * @param message is the message read from the stream.
     * @throws ProcessMessageException is the type of exception to throw in case of failure.
     */
    private void processMessage(MapRecord<String, String, String> message) throws ProcessMessageException {
        logger.debug("Processing message: {}", message.getId());
        String operation = message.getValue().get(EVENT_OPERATION_KEY);
        if (Operation.DELETE.getValue().equalsIgnoreCase(operation)) {
            deleteEntity(message.getValue().get(EVENT_CONTENT_KEY));
        } else {
            try {
                T entity = getObjectMapper().readValue(message.getValue().get(EVENT_CONTENT_KEY), getEntityClass());
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

    /***
     * This method sends an acknowledgment for the message once processing is complete and successful, ensuring that the
     * message is marked as processed in the stream.
     * @param message is the message read from the stream.
     * @throws AcknowledgeMessageException is the type of exception to throw in case of failure.
     */
    private void acknowledgeMessage(MapRecord<String, String, String> message) throws AcknowledgeMessageException {
        try {
            logger.debug("Acknowledging message: {}", message.getId());
            getRedisTemplate().opsForStream().acknowledge(getConsumerGroup(), message);
            logger.info("Acknowledged message: {}", message.getId());
        } catch (Exception e) {
            logger.error("Error acknowledging message: {}", message.getId(), e);
            // will be picked up by the processPendingMessages method
            throw new AcknowledgeMessageException(e);
        }
    }

    /***
     * The processPendingMessages method is a scheduled job that runs every 5 seconds to handle pending messages in a Redis Stream.
     * It begins by retrieving the necessary keys for the stream, consumer group, and consumer name.
     * The method then checks for any pending messages in the specified group and, if found, processes them in batches.
     * For each pending message, it retrieves the message from the stream using its ID and tracks processing attempts through a counter.
     * The method implements error handling with a retry mechanism, monitoring both the number of attempts and the elapsed time since the message was first delivered.
     * If either the maximum number of attempts is exceeded or the message has been pending for too long, it's moved to a failure handling process.
     * During normal processing, the method attempts to process each message and, upon success, acknowledges it and cleans up the attempt counter. ,
     * If processing or acknowledgment fails, the message may be retried depending on the attempt count.
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingMessages() {
        String streamKey = getStreamKey();
        String groupName = getConsumerGroup();
        String consumerName = getConsumerName();

        try {
            PendingMessagesSummary pendingSummary = getRedisTemplate().opsForStream()
                    .pending(streamKey, groupName);

            if (pendingSummary != null && pendingSummary.getTotalPendingMessages() > 0) {
                logger.info("Found {} pending messages for group {}",
                        pendingSummary.getTotalPendingMessages(), groupName);

                PendingMessages pendingMessages = getRedisTemplate().opsForStream()
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
                                    getRedisTemplate().opsForStream().range(streamKey,
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
                            if (counter > MAX_ATTEMPTS) {
                                ProcessMessageException e = new ProcessMessageException("Too many attempts");
                                handleMessageFailure(message, e, getCounterKey(message.getId().getValue()));
                                throw new RuntimeException(e);
                            }
                            if (elapsedTime > MAX_RETENTION) {
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
                                if (counter > MAX_ATTEMPTS) {
                                    handleMessageFailure(message, new RuntimeException(e), getCounterKey(message.getId().getValue()));
                                    throw new RuntimeException(e);
                                }
                            } catch (AcknowledgeMessageException e) {
                                if (counter > MAX_ATTEMPTS) {
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

    /***
     * This method handles failed pending messages by moving them to a Dead Letter Queue (DLQ) stream.
     * @param message is the message read from the stream.
     * @param cause is the exception generated during processing or acknowledging the message.
     * @param counterKey the key of the counter to expire.
     */
    private void handleMessageFailure(MapRecord<String, String, String> message,
                                      Exception cause,
                                      String counterKey) {
        handleDLQ(message, cause);
        expireCounterKey(counterKey);
    }

    /***
     * This method expires the attempt count stored as a key for a specific message.
     * @param counterKey to expire.
     * @return `true` if the TTL was set, `false` otherwise.
     */
    private Boolean expireCounterKey(String counterKey) {
        return getRedisTemplate().expire(counterKey, Duration.ZERO);
    }

    /***
     * This method increases the attempt count for a specific pending message, tracking the number of processing attempts
     * made for better error handling and retry management.
     * @param counterKey to increment.
     * @return the new incremented value.
     */
    private Long incrementCounterKey(String counterKey) {
        return getRedisTemplate().opsForValue().increment(counterKey);
    }

    /***
     * This method retrieves the key for the counter that tracks the number of processing attempts for a pending message.
     * @param id is the ID of the original message read from the pending-entry-list of the stream.
     * @return The counter key based on the stream name.
     */
    private String getCounterKey(String id) {
        return getStreamKey()+KEY_SEPARATOR+id;
    }

    /***
     * This method converts a `MapRecord<String, Object, Object>` object into a `MapRecord<String, String, String>` object.
     * @param record is the message read from the pending-entry-list of the stream.
     * @return a new `MapRecord<String, String, String>` object.
     */
    private MapRecord<String, String, String> convertMapRecord(MapRecord<String, Object, Object> record) {
        Map<String, String> convertedMap = new HashMap<>();
        record.getValue().forEach((k, v) -> {
            convertedMap.put(String.valueOf(k),String.valueOf(v));
        });

        return StreamRecords.newRecord()
                .withId(record.getId())
                .ofMap(convertedMap)
                .withStreamKey(record.getStream());
    }

    /***
     * The handleDLQ method is responsible for managing failed messages by moving them to a Dead Letter Queue (DLQ) in Redis Streams.
     * When a message fails processing beyond recovery, this method takes the original message and the exception that caused the failure,
     * then creates an enhanced version of the message with additional metadata about the failure.
     * This enhanced message includes the original content plus important diagnostic information such as the error message,
     * original stream key, message ID, consumer name, and consumer group. The method then attempts to add this enriched
     * message to a separate Dead Letter Queue stream, using the original message's ID to maintain traceability.
     * After successfully moving the message to the DLQ, it acknowledges the original message to prevent reprocessing.
     * @param message is the original message read from the stream.
     * @param e is the exception generated during processing or acknowledging the message.
     */
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

                getRedisTemplate().opsForStream().add(
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
