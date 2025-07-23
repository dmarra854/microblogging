package com.microblogging.project.application.service;

import com.microblogging.project.application.usecase.PostTweetUseCase;
import com.microblogging.project.domain.event.TweetPostedEvent;
import com.microblogging.project.domain.exception.UserNotFoundException;
import com.microblogging.project.domain.model.Tweet;
import com.microblogging.project.domain.port.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class PostTweetService implements PostTweetUseCase {

    private static final Logger log = LoggerFactory.getLogger(PostTweetService.class);

    private final TweetRepository tweetRepo;
    private final UserRepository userRepo;
    private final FollowRepository followRepo;
    private final MessagePublisher messagePublisher;
    private final TimelineCachePort timelineCachePort;

    public PostTweetService(
        TweetRepository tweetRepo,
        UserRepository userRepo,
        FollowRepository followRepo,
        MessagePublisher messagePublisher,
        TimelineCachePort timelineCachePort) {
            this.tweetRepo = tweetRepo;
            this.userRepo = userRepo;
            this.followRepo = followRepo;
            this.messagePublisher = messagePublisher;
            this.timelineCachePort = timelineCachePort;
        }

    @Override
    @Transactional
    public Tweet post(UUID userId, String content) {

        // 1. Business Rule: Check if userId exists
        if (!userRepo.existsById(userId)) {
            log.warn("Attempted to post tweet for non-existent user: {}", userId);
            throw new UserNotFoundException("User with ID " + userId + " not found.");
        }

        // 2. Content Length Check (Assuming primary validation is in TweetRequest DTO)
        // If content.length() > 280, it should ideally be caught by @Size validation on TweetRequest DTO
        // in the controller layer. If it's a core business rule and should also be enforced here,
        // you might throw a more specific domain exception like TweetContentTooLongException.
        // For simplicity, we'll assume DTO validation handles this.
        // If you want to keep it here, replace IllegalArgumentException with a domain exception:
        // if (content.length() > 280) throw new TweetContentTooLongException("Tweet content cannot exceed 280 characters");


        // 3. Create a new Tweet domain object
        Tweet tweet = new Tweet(
                UUID.randomUUID(), // Generate a new ID for the tweet
                userId,
                content,
                LocalDateTime.now() // Set the current timestamp
        );

        // 4. Persist the Tweet (using the domain port)
        tweetRepo.save(tweet);
        log.info("Tweet {} posted by user {}", tweet.getId(), tweet.getUserId());


        // 5. Invalidate timelines of followers (and potentially the user's own timeline)
        // Retrieve followers of the user who just tweeted
        Set<UUID> followers = followRepo.findFollowersByFolloweeId(userId);

        // Invalidate timeline for each follower
        for (UUID followerId : followers) {
            timelineCachePort.invalidateTimeline(followerId); // Use the cache port
            log.debug("Invalidated timeline cache for follower: {}", followerId);
        }
        // Also invalidate the tweeter's own timeline so they see their new tweet immediately
        timelineCachePort.invalidateTimeline(userId);
        log.debug("Invalidated own timeline cache for user: {}", userId);


        // 6. Publish a Domain Event for asynchronous processing
        TweetPostedEvent event = new TweetPostedEvent(
                tweet.getId(),
                tweet.getUserId(),
                tweet.getContent(),
                tweet.getCreatedAt()
        );
        messagePublisher.publishTweetPostedEvent(event);
        log.info("TweetPostedEvent published for tweet {}.", tweet.getId());

        // Return the created Tweet domain object
        return tweet;
    }
}