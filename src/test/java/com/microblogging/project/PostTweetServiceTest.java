package com.microblogging.project;

import com.microblogging.project.application.service.PostTweetService;
import com.microblogging.project.application.usecase.PostTweetUseCase;
import com.microblogging.project.domain.event.TweetPostedEvent;
import com.microblogging.project.domain.exception.UserNotFoundException;
import com.microblogging.project.domain.model.Tweet;
import com.microblogging.project.domain.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*; // Import for ArgumentCaptor and other Mockito features
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections; // For empty sets
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostTweetServiceTest {

    @Mock
    private TweetRepository tweetRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private FollowRepository followRepo;
    @Mock
    private MessagePublisher messagePublisher;
    @Mock
    private TimelineCachePort timelineCachePort;

    @InjectMocks
    private PostTweetService postTweetService;

    // ArgumentCaptor to capture objects passed to mock methods for verification
    @Captor
    private ArgumentCaptor<Tweet> tweetCaptor;
    @Captor
    private ArgumentCaptor<TweetPostedEvent> eventCaptor;
    @Captor
    private ArgumentCaptor<UUID> uuidCaptor;

    // Test data
    private UUID userId;
    private String content;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        content = "This is a test tweet content.";

        // Default mock behavior for user existence
        when(userRepo.existsById(userId)).thenReturn(true);
    }

    @Test
    @DisplayName("Should successfully post a tweet and return the created tweet")
    void post_Success() {
        // Arrange
        Set<UUID> followers = new HashSet<>();
        followers.add(UUID.randomUUID());
        followers.add(UUID.randomUUID());
        when(followRepo.findFollowersByFolloweeId(userId)).thenReturn(followers);

        // Stubbing for save is usually not needed if you only verify call
        // But if `tweetRepo.save` returns the saved entity, you might need it
        // when(tweetRepo.save(any(Tweet.class))).thenReturn(any(Tweet.class)); // Example if needed

        // Act
        Tweet resultTweet = assertDoesNotThrow(() -> postTweetService.post(userId, content));

        // Assert

        // 1. Verify Tweet persistence
        verify(tweetRepo, times(1)).save(tweetCaptor.capture());
        Tweet capturedTweet = tweetCaptor.getValue();
        assertNotNull(capturedTweet.getId()); // ID should be generated
        assertEquals(userId, capturedTweet.getUserId());
        assertEquals(content, capturedTweet.getContent());
        assertNotNull(capturedTweet.getCreatedAt()); // Timestamp should be set
        assertTrue(capturedTweet.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1))); // Check timestamp roughly

        // Ensure the returned tweet is the one that was saved
        assertEquals(capturedTweet.getId(), resultTweet.getId());
        assertEquals(capturedTweet.getUserId(), resultTweet.getUserId());
        assertEquals(capturedTweet.getContent(), resultTweet.getContent());
        assertEquals(capturedTweet.getCreatedAt(), resultTweet.getCreatedAt());


        // 2. Verify cache invalidation for followers and tweeter
        verify(followRepo, times(1)).findFollowersByFolloweeId(userId);

        // Verify invalidateTimeline called for each follower + the user themselves
        // It's called for each follower in the loop + one for the user
        verify(timelineCachePort, times(followers.size() + 1)).invalidateTimeline(uuidCaptor.capture());

        // Ensure all expected UUIDs were passed to invalidateTimeline
        Set<UUID> invalidatedUuids = new HashSet<>(uuidCaptor.getAllValues());
        assertTrue(invalidatedUuids.contains(userId)); // Tweeter's own timeline
        assertTrue(invalidatedUuids.containsAll(followers)); // All followers' timelines
        assertEquals(followers.size() + 1, invalidatedUuids.size()); // Total unique calls


        // 3. Verify event publishing
        verify(messagePublisher, times(1)).publishTweetPostedEvent(eventCaptor.capture());
        TweetPostedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(capturedTweet.getId(), capturedEvent.tweetId());
        assertEquals(capturedTweet.getUserId(), capturedEvent.userId());
        assertEquals(capturedTweet.getContent(), capturedEvent.content());
        assertEquals(capturedTweet.getCreatedAt(), capturedEvent.postedAt());

        // 4. Verify user existence check
        verify(userRepo, times(1)).existsById(userId);

        // Ensure no other unexpected interactions
        verifyNoMoreInteractions(tweetRepo, userRepo, followRepo, messagePublisher, timelineCachePort);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if posting user does not exist")
    void post_UserNotFound() {
        // Arrange
        when(userRepo.existsById(userId)).thenReturn(false); // User does not exist

        // Act & Assert
        UserNotFoundException thrown = assertThrows(
                UserNotFoundException.class,
                () -> postTweetService.post(userId, content)
        );

        assertEquals("User with ID " + userId + " not found.", thrown.getMessage());

        // Verify only user existence check was performed
        verify(userRepo, times(1)).existsById(userId);
        verifyNoInteractions(tweetRepo, followRepo, messagePublisher, timelineCachePort);
    }

    @Test
    @DisplayName("Should invalidate own timeline even if user has no followers")
    void post_NoFollowers() {
        // Arrange
        when(followRepo.findFollowersByFolloweeId(userId)).thenReturn(Collections.emptySet()); // No followers

        // Act
        assertDoesNotThrow(() -> postTweetService.post(userId, content));

        // Assert
        verify(tweetRepo, times(1)).save(any(Tweet.class)); // Tweet should still be saved
        verify(userRepo, times(1)).existsById(userId); // User existence checked

        // Only own timeline should be invalidated
        verify(timelineCachePort, times(1)).invalidateTimeline(userId);
        verify(followRepo, times(1)).findFollowersByFolloweeId(userId);
        verify(messagePublisher, times(1)).publishTweetPostedEvent(any(TweetPostedEvent.class));

        verifyNoMoreInteractions(tweetRepo, userRepo, followRepo, messagePublisher, timelineCachePort);
    }

    // Optional: Add a test for content length if you decide to implement that business rule in the service layer
    // @Test
    // @DisplayName("Should throw TweetContentTooLongException if content exceeds 280 characters")
    // void post_ContentTooLong() {
    //     // Arrange
    //     String longContent = "a".repeat(281); // More than 280 characters
    //
    //     // Act & Assert
    //     // Replace with your custom exception if you create one, e.g., TweetContentTooLongException
    //     IllegalArgumentException thrown = assertThrows(
    //             IllegalArgumentException.class, // Or your custom exception
    //             () -> postTweetService.post(userId, longContent)
    //     );
    //
    //     assertEquals("Tweet content cannot exceed 280 characters", thrown.getMessage());
    //     verify(userRepo, times(1)).existsById(userId); // User check should still happen
    //     verifyNoInteractions(tweetRepo, followRepo, messagePublisher, timelineCachePort); // No other interactions
    // }
}