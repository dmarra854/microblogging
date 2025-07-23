package com.microblogging.project.domain.port;

import com.microblogging.project.domain.model.Tweet;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TweetRepository {
    void save(Tweet tweet);
    List<Tweet> findAllByUserIds(Set<UUID> userIds);
}
