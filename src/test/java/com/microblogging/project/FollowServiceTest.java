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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito annotations
class FollowServiceTest {

    @Mock // Creates a mock instance of UserRepository
    private UserRepository userRepository;

    @Mock // Creates a mock instance of FollowRepository
    private FollowRepository followRepository;

    @InjectMocks // Injects the mocks into an instance of FollowService
    private FollowService followService;

    // Test UUIDs for consistent testing
    private UUID followerId;
    private UUID followeeId;
    private UUID nonExistentUserId;

    @BeforeEach
    void setUp() {
        // Initialize UUIDs before each test
        followerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        followeeId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        nonExistentUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        // Common setup for user existence: assume both follower and followee exist by default
        when(userRepository.existsById(followerId)).thenReturn(true);
        when(userRepository.existsById(followeeId)).thenReturn(true);
        when(userRepository.existsById(nonExistentUserId)).thenReturn(false); // Explicitly state non-existent
    }

    @Test
    @DisplayName("Should successfully follow a user")
    void shouldSuccessfullyFollowUser() {
        // Arrange
        // Assume follower is not yet following followee
        when(followRepository.exists(followerId, followeeId)).thenReturn(false);
        doNothing().when(followRepository).save(followerId, followeeId); // Mock the save method

        // Act
        followService.follow(followerId, followeeId);

        // Assert
        // Verify that userRepository.existsById was called for both users
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        // Verify that followRepository.exists was called
        verify(followRepository, times(1)).exists(followerId, followeeId);
        // Verify that followRepository.save was called exactly once with the correct arguments
        verify(followRepository, times(1)).save(followerId, followeeId);
    }

    @Test
    @DisplayName("Should throw CannotFollowSelfException when followerId equals followeeId")
    void shouldThrowCannotFollowSelfException() {
        // Arrange & Act & Assert
        CannotFollowSelfException thrown = assertThrows(CannotFollowSelfException.class, () -> {
            followService.follow(followerId, followerId); // Try to follow self
        });

        assertEquals("Users cannot follow themselves.", thrown.getMessage());
        // Verify no interactions with repositories as the check happens first
        verifyNoInteractions(userRepository);
        verifyNoInteractions(followRepository);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when follower does not exist")
    void shouldThrowUserNotFoundWhenFollowerNotFound() {
        // Arrange
        when(userRepository.existsById(followerId)).thenReturn(false); // Simulate follower not found

        // Act & Assert
        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            followService.follow(followerId, followeeId);
        });

        assertEquals("Follower user with ID " + followerId + " not found.", thrown.getMessage());
        // Verify only follower existence check was made
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, never()).existsById(followeeId); // Should not check followee if follower not found
        verifyNoInteractions(followRepository);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when followee does not exist")
    void shouldThrowUserNotFoundWhenFolloweeNotFound() {
        // Arrange
        when(userRepository.existsById(followeeId)).thenReturn(false); // Simulate followee not found

        // Act & Assert
        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            followService.follow(followerId, followeeId);
        });

        assertEquals("Followee user with ID " + followeeId + " not found.", thrown.getMessage());
        // Verify both user existence checks were made up to the point of followee check
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verifyNoInteractions(followRepository);
    }

    @Test
    @DisplayName("Should throw AlreadyFollowingException when user is already following")
    void shouldThrowAlreadyFollowingException() {
        // Arrange
        when(followRepository.exists(followerId, followeeId)).thenReturn(true); // Simulate already following

        // Act & Assert
        AlreadyFollowingException thrown = assertThrows(AlreadyFollowingException.class, () -> {
            followService.follow(followerId, followeeId);
        });

        assertEquals("User " + followerId + " is already following user " + followeeId + ".", thrown.getMessage());
        // Verify existence checks and followRepository.exists were called
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verify(followRepository, times(1)).exists(followerId, followeeId);
        // Verify save was NOT called
        verify(followRepository, never()).save(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("Should successfully unfollow a user")
    void shouldSuccessfullyUnfollowUser() {
        // Arrange
        // Assume follower is currently following followee
        when(followRepository.exists(followerId, followeeId)).thenReturn(true);
        doNothing().when(followRepository).delete(followerId, followeeId); // Mock the delete method

        // Act
        followService.unfollow(followerId, followeeId);

        // Assert
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verify(followRepository, times(1)).exists(followerId, followeeId);
        verify(followRepository, times(1)).delete(followerId, followeeId);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when trying to unfollow self")
    void shouldThrowIllegalArgumentExceptionWhenUnfollowSelf() {
        // Arrange & Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            followService.unfollow(followerId, followerId); // Try to unfollow self
        });

        assertEquals("Cannot unfollow self. This operation is nonsensical.", thrown.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(followRepository);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when unfollower does not exist")
    void shouldThrowUserNotFoundWhenUnfollowerNotFound() {
        // Arrange
        when(userRepository.existsById(followerId)).thenReturn(false); // Simulate unfollower not found

        // Act & Assert
        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            followService.unfollow(followerId, followeeId);
        });

        assertEquals("Follower user with ID " + followerId + " not found.", thrown.getMessage());
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, never()).existsById(followeeId);
        verifyNoInteractions(followRepository);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when unfollowee does not exist")
    void shouldThrowUserNotFoundWhenUnfolloweeNotFound() {
        // Arrange
        when(userRepository.existsById(followeeId)).thenReturn(false); // Simulate unfollowee not found

        // Act & Assert
        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            followService.unfollow(followerId, followeeId);
        });

        assertEquals("Followee user with ID " + followeeId + " not found.", thrown.getMessage());
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verifyNoInteractions(followRepository);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when not currently following during unfollow")
    void shouldThrowIllegalArgumentExceptionWhenNotFollowing() {
        // Arrange
        when(followRepository.exists(followerId, followeeId)).thenReturn(false); // Simulate not following

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            followService.unfollow(followerId, followeeId);
        });

        assertEquals("User " + followerId + " is not currently following user " + followeeId + ".", thrown.getMessage());
        verify(userRepository, times(1)).existsById(followerId);
        verify(userRepository, times(1)).existsById(followeeId);
        verify(followRepository, times(1)).exists(followerId, followeeId);
        verify(followRepository, never()).delete(any(UUID.class), any(UUID.class));
    }
}