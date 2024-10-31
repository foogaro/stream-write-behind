package com.foogaro.redis.wbs.core;

public class Misc {

    public final static String EVENT_CONTENT_KEY = "content";
    public final static String EVENT_OPERATION_KEY = "operation";

    public final static String KEY_SEPARATOR = ":";
    public final static String VALUE_SEPARATOR = "_";

    private final static String STREAM_KEY_PREFIX = "wb:stream:entity:";
    private final static String STREAM_KEY_DLQ_SUFFIX = ":dlq";

    public final static String CONSUMER_GROUP_SUFFIX = "_group";
    public final static String CONSUMER_SUFFIX = "_consumer";

    public static String getStreamKey(Class<?> clazz) {
        return STREAM_KEY_PREFIX + clazz.getSimpleName().toLowerCase();
    }

    public static String getDLQStreamKey(Class<?> clazz) {
        return STREAM_KEY_PREFIX + clazz.getSimpleName().toLowerCase() + STREAM_KEY_DLQ_SUFFIX;
    }

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

        public static Operation fromString(String text) {
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
