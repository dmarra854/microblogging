package com.microblogging.project.domain.port;

import com.microblogging.project.domain.event.TweetPostedEvent; // Import your domain event

public interface MessagePublisher {

    /**
     * Publishes an event indicating that a tweet has been successfully posted.
     *
     * @param event The TweetPostedEvent to publish.
     */
    void publishTweetPostedEvent(TweetPostedEvent event);
}
