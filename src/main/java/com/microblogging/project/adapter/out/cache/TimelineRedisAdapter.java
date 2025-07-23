package com.microblogging.project.adapter.out.cache;

import com.microblogging.project.domain.model.Tweet;
import com.microblogging.project.domain.port.TimelineCachePort; // Import the domain port
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TimelineRedisAdapter implements TimelineCachePort {

    private static final Logger log = LoggerFactory.getLogger(TimelineRedisAdapter.class);

    private final RedisTemplate<String, Tweet> redisTemplate;
    private final ListOperations<String, Tweet> listOperations;

    public TimelineRedisAdapter(RedisTemplate<String, Tweet> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.listOperations = redisTemplate.opsForList();
    }

    private String getRedisKey(UUID userId) {
        return "timeline:" + userId;
    }

    @Override
    public Optional<List<Tweet>> getTimeline(UUID userId) {
        String redisKey = getRedisKey(userId);
        try {
            List<Tweet> cachedTweets = listOperations.range(redisKey, 0, 49);
            if (cachedTweets != null && !cachedTweets.isEmpty()) {
                return Optional.of(cachedTweets);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve timeline from Redis for user {}: {}", userId, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void cacheTimeline(UUID userId, List<Tweet> tweets, Duration duration) {
        String redisKey = getRedisKey(userId);
        try {
            redisTemplate.delete(redisKey);
            listOperations.rightPushAll(redisKey, tweets);
            redisTemplate.expire(redisKey, duration);
            log.debug("Timeline cached in Redis for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to push timeline to Redis for user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void invalidateTimeline(UUID userId) {
        String redisKey = getRedisKey(userId);
        try {
            redisTemplate.delete(redisKey);
            log.debug("Timeline invalidated in Redis for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate timeline in Redis for user {}: {}", userId, e.getMessage());
        }
    }
}