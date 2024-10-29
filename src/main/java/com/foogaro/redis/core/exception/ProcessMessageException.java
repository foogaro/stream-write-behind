package com.foogaro.redis.core.exception;

public class ProcessMessageException extends Exception {

    public ProcessMessageException() {
    }

    public ProcessMessageException(String message) {
        super(message);
    }

    public ProcessMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessMessageException(Throwable cause) {
        super(cause);
    }

    public ProcessMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
