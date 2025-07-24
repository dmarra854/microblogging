package com.microblogging.project;

import com.microblogging.project.application.service.FollowService;
import com.microblogging.project.domain.exception.AlreadyFollowingException;
import com.microblogging.project.domain.exception.CannotFollowSelfException;
import com.microblogging.project.domain.exception.UserNotFoundException;
import com.microblogging.project.domain.port.FollowRepository;
import com.microblogging.project.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito; // Import Mockito class for reset()

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*; // Import static methods for Mockito

@ExtendWith(MockitoExtension.class) // Integrates Mockito with JUnit 5
class FollowServiceTest {

    @Mock // Creates a mock instance of UserRepository
    private UserRepository userRepository;

    @Mock // Creates a mock instance of FollowRepository
    private FollowRepository followRepository;

    @InjectMocks // Injects the mocks into a new instance of FollowService
    private FollowService followService;

    // UUIDs for testing
    private UUID followerId;
    private UUID followeeId;
    private UUID nonExistentUserId;

    @BeforeEach // This method runs before each test method
    void setUp() {
        followerId = UUID.randomUUID();
        followeeId = UUID.randomUUID();
        nonExistentUserId = UUID.randomUUID();

        // Default mock behaviors for common scenarios
        // Assume users exist by default unless explicitly changed in a specific test
        when(userRepository.existsById(followerId)).thenReturn(true);
        when(userRepository.existsById(followeeId)).thenReturn(true);
    }

    // --- Tests for 'follow' method ---

    @Test
    @DisplayName("Should successfully follow a user when all conditions are met")
    void follow_Success() {
        // Arrange
        // Default setUp makes users exist.
        // Assume they are not already following
        when(followRepository.exists(followerId, followeeId)).thenReturn(false);

        // Act
        assertDoesNotThrow(() -> followService.follow(followerId, followeeId));

        // Assert
        // Verify that save was called exactly once with the correct IDs
        verify(followRepository, times(1)).save(followerId, followeeId);
        // Verify that exists was called for both users
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        // Verify that exists was called for the follow relationship
        verify(followRepository, times(1)).exists(followerId, followeeId);
    }

    @Test
    @DisplayName("Should throw CannotFollowSelfException if followerId equals followeeId in follow()")
    void follow_CannotFollowSelf() {
        // Arrange
        // Reset mocks for this specific test where no user interactions are expected
        // This prevents UnnecessaryStubbingException from setUp's 'when' calls
        Mockito.reset(userRepository, followRepository);

        UUID selfId = followerId; // Use followerId for simplicity, but could be a new UUID

        // Act & Assert
        CannotFollowSelfException thrown = assertThrows(
                CannotFollowSelfException.class,
                () -> followService.follow(selfId, selfId)
        );

        assertEquals("Users cannot follow themselves.", thrown.getMessage());
        // Verify that no repository methods were called
        verifyNoInteractions(userRepository, followRepository);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if follower does not exist in follow()")
    void follow_FollowerNotFound() {
        // Arrange
        when(userRepository.existsById(followerId)).thenReturn(false); // Follower does not exist

        // Act & Assert
        UserNotFoundException thrown = assertThrows(
                UserNotFoundException.class,
                () -> followService.follow(followerId, followeeId)
        );

        assertEquals("Follower user with ID " + followerId + " not found.", thrown.getMessage());
        // Verify only existsById for follower was called
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, never()).existsById(followeeId); // Should not check followee
        verifyNoInteractions(followRepository); // No followRepository interactions
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if followee does not exist in follow()")
    void follow_FolloweeNotFound() {
        // Arrange
        when(userRepository.existsById(followeeId)).thenReturn(false); // Followee does not exist

        // Act & Assert
        UserNotFoundException thrown = assertThrows(
                UserNotFoundException.class,
                () -> followService.follow(followerId, followeeId)
        );

        assertEquals("Followee user with ID " + followeeId + " not found.", thrown.getMessage());
        // Verify both existsById were called
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verifyNoInteractions(followRepository); // No followRepository interactions
    }

    @Test
    @DisplayName("Should throw AlreadyFollowingException if user is already following")
    void follow_AlreadyFollowing() {
        // Arrange
        when(followRepository.exists(followerId, followeeId)).thenReturn(true); // Already following

        // Act & Assert
        AlreadyFollowingException thrown = assertThrows(
                AlreadyFollowingException.class,
                () -> followService.follow(followerId, followeeId)
        );

        assertEquals("User " + followerId + " is already following user " + followeeId + ".", thrown.getMessage());
        // Verify the calls up to the exists check
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verify(followRepository, times(1)).exists(followerId, followeeId);
        verify(followRepository, never()).save(any(UUID.class), any(UUID.class)); // save should NOT be called
    }

    // --- Tests for 'unfollow' method ---

    @Test
    @DisplayName("Should successfully unfollow a user")
    void unfollow_Success() {
        // Arrange
        // Assume they are currently following
        when(followRepository.exists(followerId, followeeId)).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> followService.unfollow(followerId, followeeId));

        // Assert
        verify(followRepository, times(1)).delete(followerId, followeeId);
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verify(followRepository, times(1)).exists(followerId, followeeId);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if followerId equals followeeId in unfollow()")
    void unfollow_CannotUnfollowSelf() {
        // Arrange
        // Reset mocks for this specific test where no user interactions are expected
        Mockito.reset(userRepository, followRepository);

        UUID selfId = followerId;

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> followService.unfollow(selfId, selfId)
        );

        assertEquals("Cannot unfollow self. This operation is nonsensical.", thrown.getMessage());
        verifyNoInteractions(userRepository, followRepository);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if follower does not exist in unfollow()")
    void unfollow_FollowerNotFound() {
        // Arrange
        when(userRepository.existsById(followerId)).thenReturn(false);

        // Act & Assert
        UserNotFoundException thrown = assertThrows(
                UserNotFoundException.class,
                () -> followService.unfollow(followerId, followeeId)
        );

        assertEquals("Follower user with ID " + followerId + " not found.", thrown.getMessage());
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, never()).existsById(followeeId);
        verifyNoInteractions(followRepository);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if followee does not exist in unfollow()")
    void unfollow_FolloweeNotFound() {
        // Arrange
        when(userRepository.existsById(followeeId)).thenReturn(false);

        // Act & Assert
        UserNotFoundException thrown = assertThrows(
                UserNotFoundException.class,
                () -> followService.unfollow(followerId, followeeId)
        );

        assertEquals("Followee user with ID " + followeeId + " not found.", thrown.getMessage());
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verifyNoInteractions(followRepository);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if user is not currently following when unfollowing")
    void unfollow_NotCurrentlyFollowing() {
        // Arrange
        when(followRepository.exists(followerId, followeeId)).thenReturn(false); // Not following

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> followService.unfollow(followerId, followeeId)
        );

        assertEquals("User " + followerId + " is not currently following user " + followeeId + ".", thrown.getMessage());
        // Verify calls up to the exists check
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verify(followRepository, times(1)).exists(followerId, followeeId);
        verify(followRepository, never()).delete(any(UUID.class), any(UUID.class)); // delete should NOT be called
    }
}