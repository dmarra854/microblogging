package com.microblogging.project.domain.port;

import com.microblogging.project.domain.model.Tweet;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimelineCachePort {
    Optional<List<Tweet>> getTimeline(UUID userId);
    void cacheTimeline(UUID userId, List<Tweet> tweets, Duration duration);
    void invalidateTimeline(UUID userId);
}