package com.foogaro.redis.wbs.core.exception;

/***
 * Checked exception for managing the acknowledgement of the message.
 */
public class AcknowledgeMessageException extends Exception {

    public AcknowledgeMessageException() {
    }

    public AcknowledgeMessageException(String message) {
        super(message);
    }

    public AcknowledgeMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public AcknowledgeMessageException(Throwable cause) {
        super(cause);
    }

    public AcknowledgeMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
