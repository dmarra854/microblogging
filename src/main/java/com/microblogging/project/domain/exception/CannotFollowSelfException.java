package com.microblogging.project.domain.exception;
/**
 * Exception thrown when a user attempts to follow themselves.
 */
public class CannotFollowSelfException extends RuntimeException {

    /**
     * Constructs a new CannotFollowSelfException with the specified detail message.
     *
     * @param message the detail message.
     */
    public CannotFollowSelfException(String message) {
        super(message);
    }

    /**
     * Constructs a new CannotFollowSelfException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause (which is saved for later retrieval by the Throwable.getCause() method).
     */
    public CannotFollowSelfException(String message, Throwable cause) {
        super(message, cause);
    }
}