package com.microblogging.project.domain.exception;

/**
 * Exception thrown when a user attempts to follow another user they are already following.
 */
public class AlreadyFollowingException extends RuntimeException {

    /**
     * Constructs a new AlreadyFollowingException with the specified detail message.
     *
     * @param message the detail message.
     */
    public AlreadyFollowingException(String message) {
        super(message);
    }

    /**
     * Constructs a new AlreadyFollowingException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause (which is saved for later retrieval by the Throwable.getCause() method).
     */
    public AlreadyFollowingException(String message, Throwable cause) {
        super(message, cause);
    }
}