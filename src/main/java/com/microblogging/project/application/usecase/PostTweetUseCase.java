package com.microblogging.project.application.usecase;

import com.microblogging.project.domain.model.Tweet; // NEW: Import the Tweet domain model
import java.util.UUID;

// This is the inbound port (use case interface) for posting tweets
public interface PostTweetUseCase {
    /**
     * Handles the use case of posting a new tweet by a user.
     *
     * @param userId  The ID of the user posting the tweet.
     * @param content The content of the tweet.
     * @return The newly created Tweet domain object.
     */
    Tweet post(UUID userId, String content); // REFACTORED: Changed return type from void to Tweet
}
