package com.microblogging.project.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a domain event indicating that a new tweet has been successfully posted.
 * This event captures key immutable information about the tweet at the time it was posted.
 *
 * Using a Java record for immutability and conciseness, which is ideal for domain events.
 *
 * @param tweetId  The unique identifier of the tweet that was posted.
 * @param userId   The unique identifier of the user who posted the tweet.
 * @param content  The actual text content of the tweet.
 * @param postedAt The timestamp when the tweet was posted.
 */
public record TweetPostedEvent(
        UUID tweetId,
        UUID userId,
        String content,
        LocalDateTime postedAt
) {
    // Records automatically generate:
    // - A canonical constructor (with all components as parameters)
    // - Getter methods for all components (e.g., tweetId(), userId())
    // - equals(), hashCode(), and toString() implementations based on all components.

    // No additional code is typically needed here for a simple event.
    // If you need custom validation or behavior, you can add compact constructor or methods.
    // For example:
    /*
    public TweetPostedEvent {
        if (tweetId == null || userId == null || content == null || postedAt == null) {
            throw new IllegalArgumentException("All fields must be non-null for TweetPostedEvent.");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("Tweet content cannot be blank.");
        }
    }
    */
}