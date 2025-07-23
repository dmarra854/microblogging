package com.microblogging.project.domain.port;

import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface FollowRepository {
    void save(UUID followerId, UUID followeeId);
    void delete(UUID followerId, UUID followeeId);
    boolean exists(UUID followerId, UUID followeeId);
    Set<UUID> findFollowersByFolloweeId(UUID followeeId);
    Set<UUID> findFollowees(UUID followerId); // To get who a user follows
}