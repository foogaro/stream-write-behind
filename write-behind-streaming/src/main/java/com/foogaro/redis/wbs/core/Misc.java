package com.foogaro.redis.wbs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;

public class Misc {

    private static final Logger logger = LoggerFactory.getLogger(Misc.class);

    public final static String EVENT_CONTENT_KEY = "content";
    public final static String EVENT_OPERATION_KEY = "operation";

    public final static String KEY_SEPARATOR = ":";
    public final static String VALUE_SEPARATOR = "_";

    private final static String STREAM_KEY_PREFIX = "wb:stream:entity:";
    private final static String STREAM_KEY_DLQ_SUFFIX = ":dlq";

    public final static String CONSUMER_GROUP_SUFFIX = "_group";
    public final static String CONSUMER_SUFFIX = "_consumer";

    public static String getStreamKey(final Class<?> entityClass) {
        return STREAM_KEY_PREFIX + entityClass.getSimpleName().toLowerCase();
    }

    public static String getDLQStreamKey(final Class<?> entityClass) {
        return STREAM_KEY_PREFIX + entityClass.getSimpleName().toLowerCase() + STREAM_KEY_DLQ_SUFFIX;
    }

    public static String getConsumerGroup(final Class<?> repositoryClass) {
        return repositoryClass.getSimpleName().toLowerCase() + CONSUMER_GROUP_SUFFIX;
    }

    public static String getConsumerName(final Class<?> entityClass, final Class<?> repositoryClass) {
        return entityClass.getSimpleName().toLowerCase() + VALUE_SEPARATOR + repositoryClass.getSimpleName().toLowerCase() + CONSUMER_SUFFIX;
    }

    public static void dumpMessage(final MapRecord<String, String, String> message) {
        try {
            logger.debug("Stream ID: {}", message.getStream());
            logger.debug("Message ID: {}", message.getId());
            logger.debug("Message.Content: {}", message.getValue().get(EVENT_CONTENT_KEY));
            logger.debug("Message.Operation: {}", message.getValue().get(EVENT_OPERATION_KEY));
        } catch (Exception e) {
            logger.error("Error dumping message: {}", message, e);
        }
    }

//    public static MapRecord<String, String, String> convertMapRecord(MapRecord<String, Object, Object> record) {
//        Map<String, String> convertedMap = new HashMap<>();
//        record.getValue().forEach((k, v) -> {
//            convertedMap.put(String.valueOf(k),String.valueOf(v));
//        });
//
//        return StreamRecords.newRecord()
//                .withId(record.getId())
//                .ofMap(convertedMap)
//                .withStreamKey(record.getStream());
//    }

    public enum Operation {
        CREATE("CREATE"),
        READ("READ"),
        UPDATE("UPDATE"),
        DELETE("DELETE");

        private final String value;

        Operation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Operation fromString(final String text) {
            for (Operation operation : Operation.values()) {
                if (operation.value.equalsIgnoreCase(text)) {
                    return operation;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
