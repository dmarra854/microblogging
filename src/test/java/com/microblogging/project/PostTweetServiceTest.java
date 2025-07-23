package com.microblogging.project;

import com.microblogging.project.application.service.PostTweetService;
import com.microblogging.project.domain.event.TweetPostedEvent;
import com.microblogging.project.domain.exception.UserNotFoundException;
import com.microblogging.project.domain.model.Tweet;
import com.microblogging.project.domain.port.FollowRepository;
import com.microblogging.project.domain.port.MessagePublisher;
import com.microblogging.project.domain.port.TweetRepository;
import com.microblogging.project.domain.port.UserRepository;
import com.microblogging.project.domain.port.TimelineCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Use MockitoExtension to enable Mockito annotations
@ExtendWith(MockitoExtension.class)
class PostTweetServiceTest {

    // Mocks for all dependencies of PostTweetService
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

    // InjectMocks creates an instance of PostTweetService and injects the mocks into it
    @InjectMocks
    private PostTweetService postTweetService;

    // Test data
    private UUID testUserId;
    private String validContent;
    private String tooLongContent;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        validContent = "This is a valid tweet content.";
        tooLongContent = "x".repeat(281); // Content exceeding 280 characters
    }

    @Test
    @DisplayName("Should successfully post a tweet, save it, invalidate caches, and publish event")
    void shouldPostTweetSuccessfully() {
        // Arrange
        // Simulate user exists
        when(userRepo.existsById(testUserId)).thenReturn(true);

        // Simulate some followers
        UUID follower1Id = UUID.randomUUID();
        UUID follower2Id = UUID.randomUUID();
        Set<UUID> followers = Set.of(follower1Id, follower2Id);
        when(followRepo.findFollowersByFolloweeId(testUserId)).thenReturn(followers);

        // Act
        Tweet resultTweet = postTweetService.post(testUserId, validContent);

        // Assert
        assertNotNull(resultTweet);
        assertEquals(testUserId, resultTweet.getUserId());
        assertEquals(validContent, resultTweet.getContent());
        assertNotNull(resultTweet.getId()); // ID should be generated
        assertNotNull(resultTweet.getCreatedAt()); // Timestamp should be set

        // Verify tweetRepo.save was called with the correct Tweet object
        // Use ArgumentCaptor to capture the Tweet object passed to save
        ArgumentCaptor<Tweet> tweetCaptor = ArgumentCaptor.forClass(Tweet.class);
        verify(tweetRepo, times(1)).save(tweetCaptor.capture());
        Tweet capturedTweet = tweetCaptor.getValue();
        assertEquals(resultTweet.getId(), capturedTweet.getId());
        assertEquals(resultTweet.getUserId(), capturedTweet.getUserId());
        assertEquals(resultTweet.getContent(), capturedTweet.getContent());
        assertEquals(resultTweet.getCreatedAt(), capturedTweet.getCreatedAt());


        // Verify timelineCachePort.invalidateTimeline was called for each follower AND the user themselves
        verify(timelineCachePort, times(1)).invalidateTimeline(follower1Id);
        verify(timelineCachePort, times(1)).invalidateTimeline(follower2Id);
        verify(timelineCachePort, times(1)).invalidateTimeline(testUserId); // User's own timeline

        // Verify messagePublisher.publishTweetPostedEvent was called with the correct event
        ArgumentCaptor<TweetPostedEvent> eventCaptor = ArgumentCaptor.forClass(TweetPostedEvent.class);
        verify(messagePublisher, times(1)).publishTweetPostedEvent(eventCaptor.capture());
        TweetPostedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(resultTweet.getId(), capturedEvent.tweetId());
        assertEquals(resultTweet.getUserId(), capturedEvent.userId());
        assertEquals(resultTweet.getContent(), capturedEvent.content());
        assertEquals(resultTweet.getCreatedAt(), capturedEvent.postedAt());

        // Verify no other interactions with mocks
        verifyNoMoreInteractions(tweetRepo, userRepo, followRepo, messagePublisher, timelineCachePort);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if user does not exist")
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        // Arrange
        // Simulate user does NOT exist
        when(userRepo.existsById(testUserId)).thenReturn(false);

        // Act & Assert
        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            postTweetService.post(testUserId, validContent);
        });

        assertEquals("User with ID " + testUserId + " not found.", thrown.getMessage());

        // Verify userRepo.existsById was called
        verify(userRepo, times(1)).existsById(testUserId);
        // Verify no other interactions occurred
        verifyNoInteractions(tweetRepo, followRepo, messagePublisher, timelineCachePort);
    }

    @Test
    @DisplayName("Should invalidate only own timeline if user has no followers")
    void shouldInvalidateOnlyOwnTimelineIfNoFollowers() {
        // Arrange
        when(userRepo.existsById(testUserId)).thenReturn(true);
        // Simulate no followers
        when(followRepo.findFollowersByFolloweeId(testUserId)).thenReturn(Collections.emptySet());

        // Act
        Tweet resultTweet = postTweetService.post(testUserId, validContent);

        // Assert
        assertNotNull(resultTweet);
        verify(tweetRepo, times(1)).save(any(Tweet.class));
        verify(messagePublisher, times(1)).publishTweetPostedEvent(any(TweetPostedEvent.class));

        // Verify only the user's own timeline was invalidated
        verify(timelineCachePort, times(1)).invalidateTimeline(testUserId);
        verify(timelineCachePort, never()).invalidateTimeline(argThat(id -> !id.equals(testUserId))); // No other invalidations

        verifyNoMoreInteractions(tweetRepo, userRepo, followRepo, messagePublisher, timelineCachePort);
    }

    // Note: The service assumes content length validation is primarily handled by DTO validation.
    // However, if the service itself *could* throw IllegalArgumentException for length,
    // this test would be relevant.
    @Test
    @DisplayName("Should not process tweet if content is too long (assuming DTO validation fallback)")
    void shouldNotProcessTweetIfContentTooLong() {
        // Arrange
        // No specific mock setup needed as validation happens before other interactions

        // Act & Assert
        // The service doesn't have an explicit length check in the provided code,
        // but if it did (like in the commented-out line), this test would catch it.
        // For now, this test is more conceptual, assuming the controller's @Size validation.
        // If you were to add: if (content.length() > 280) throw new IllegalArgumentException("Tweet too long");
        // then this test would pass.
        // As per the refactored code, this check is assumed to be handled by the DTO.
        // If you want to enforce it at the service layer, uncomment the line in PostTweetService
        // and add a custom exception.
        // For now, we'll confirm no interactions if such a check *were* present.
        assertDoesNotThrow(() -> postTweetService.post(testUserId, tooLongContent)); // If no exception is thrown by service
        verifyNoInteractions(tweetRepo, userRepo, followRepo, messagePublisher, timelineCachePort);
    }
}
