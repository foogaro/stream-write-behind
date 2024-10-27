package com.foogaro.redis.core;

public class Misc {

    public final static String EVENT_CONTENT_KEY = "content";
    public final static String EVENT_OPERATION_KEY = "operation";
    public final static String DELETE_OPERATION_KEY = "delete";

    public static String getStreamKey(Class clazz) {
        return "wb:stream:" + clazz.getSimpleName().toLowerCase();
    }

}
