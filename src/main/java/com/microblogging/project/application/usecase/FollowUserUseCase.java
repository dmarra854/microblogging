package com.microblogging.project.application.usecase;

import java.util.UUID;

public interface FollowUserUseCase {
    void follow(UUID followerId, UUID followeeId);
    void unfollow(UUID followerId, UUID followeeId);
}