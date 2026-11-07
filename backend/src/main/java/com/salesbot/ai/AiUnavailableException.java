package com.salesbot.ai;

/**
 * The provider could not produce a reply (timeout, rate limit, outage, empty answer).
 * Callers decide what the customer sees instead.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiUnavailableException(String message) {
        super(message);
    }
}
