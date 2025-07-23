package com.microblogging.project.application.service;

import com.microblogging.project.application.usecase.TimelineQuery;
import com.microblogging.project.domain.exception.UserNotFoundException;
import com.microblogging.project.domain.model.Tweet;
import com.microblogging.project.domain.port.FollowRepository;
import com.microblogging.project.domain.port.TimelineCachePort;
import com.microblogging.project.domain.port.TweetRepository;
import com.microblogging.project.domain.port.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class TimelineService implements TimelineQuery {

    private static final Logger log = LoggerFactory.getLogger(TimelineService.class);

    private final TweetRepository tweetRepo;
    private final FollowRepository followRepo;
    private final UserRepository userRepo;
    private final TimelineCachePort timelineCachePort;

    public TimelineService(
            TweetRepository tweetRepo,
            FollowRepository followRepo,
            UserRepository userRepo,
            TimelineCachePort timelineCachePort
    ) {
        this.tweetRepo = tweetRepo;
        this.followRepo = followRepo;
        this.userRepo = userRepo;
        this.timelineCachePort = timelineCachePort;
    }

    @Override
    public List<Tweet> getTimeline(UUID userId) {
        if (!userRepo.existsById(userId)) {
            throw new UserNotFoundException("User with ID " + userId + " not found.");
        }

        Optional<List<Tweet>> cachedTweets = timelineCachePort.getTimeline(userId);
        if (cachedTweets.isPresent() && !cachedTweets.get().isEmpty()) {
            log.debug("Timeline retrieved from cache for user {}", userId);
            return cachedTweets.get();
        }

        Set<UUID> followees = new HashSet<>(followRepo.findFollowees(userId));
        followees.add(userId);

        List<Tweet> tweets = tweetRepo.findAllByUserIds(followees).stream()
                .sorted(Comparator.comparing(Tweet::getCreatedAt).reversed())
                .limit(50)
                .toList();

        if (!tweets.isEmpty()) {
            timelineCachePort.cacheTimeline(userId, tweets, Duration.ofMinutes(5));
        }

        log.debug("Timeline retrieved from database for user {}", userId);
        return tweets;
    }
}
