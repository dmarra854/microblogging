package com.microblogging.project.application.service;

import com.microblogging.project.application.usecase.FollowUserUseCase;
import com.microblogging.project.domain.exception.AlreadyFollowingException;
import com.microblogging.project.domain.exception.CannotFollowSelfException;
import com.microblogging.project.domain.exception.UserNotFoundException;
import com.microblogging.project.domain.port.FollowRepository;
import com.microblogging.project.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FollowService implements FollowUserUseCase {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public FollowService(UserRepository userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    @Override
    @Transactional
    public void follow(UUID followerId, UUID followeeId) {
        // 1. Business Rule: A user cannot follow themselves
        if (followerId.equals(followeeId)) {
            throw new CannotFollowSelfException("Users cannot follow themselves.");
        }

        // 2. Business Rule: Check if both users exist
        if (!userRepository.existsById(followerId)) {
            throw new UserNotFoundException("Follower user with ID " + followerId + " not found.");
        }
        if (!userRepository.existsById(followeeId)) {
            throw new UserNotFoundException("Followee user with ID " + followeeId + " not found.");
        }

        // 3. Business Rule: Check if already following
        if (followRepository.exists(followerId, followeeId)) {
            throw new AlreadyFollowingException("User " + followerId + " is already following user " + followeeId + ".");
        }

        // 4. Perform the follow operation (uses the domain port)
        followRepository.save(followerId, followeeId);
        // Additional logic could include:
        // - Publishing an event: UserFollowedEvent (for notification, activity feed, etc.)
    }

    @Override
    @Transactional
    public void unfollow(UUID followerId, UUID followeeId) {
        // 1. Business Rule: A user cannot unfollow themselves (implicitly, as they can't follow themselves)
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("Cannot unfollow self. This operation is nonsensical."); // Or a more specific exception
        }

        // 2. Business Rule: Check if both users exist (optional, but good for robust API)
        if (!userRepository.existsById(followerId)) {
            throw new UserNotFoundException("Follower user with ID " + followerId + " not found.");
        }
        if (!userRepository.existsById(followeeId)) {
            throw new UserNotFoundException("Followee user with ID " + followeeId + " not found.");
        }

        // 3. Check if currently following (necessary before unfollowing)
        if (!followRepository.exists(followerId, followeeId)) {
            // Depending on desired behavior, you might throw an exception
            // like NotFollowingException or simply return/do nothing.
            // For an explicit API, throwing an exception is usually clearer.
            throw new IllegalArgumentException("User " + followerId + " is not currently following user " + followeeId + ".");
        }

        // 4. Perform the unfollow operation
        followRepository.delete(followerId, followeeId);
        // Additional logic could include:
        // - Publishing an event: UserUnfollowedEvent
    }
}
