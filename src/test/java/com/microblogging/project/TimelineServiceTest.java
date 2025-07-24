package com.microblogging.project;

import com.microblogging.project.application.service.TimelineService;
import com.microblogging.project.domain.exception.UserNotFoundException;
import com.microblogging.project.domain.model.Tweet;
import com.microblogging.project.domain.port.FollowRepository;
import com.microblogging.project.domain.port.TimelineCachePort;
import com.microblogging.project.domain.port.TweetRepository;
import com.microblogging.project.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimelineServiceTest {

    @Mock
    private TweetRepository tweetRepo;
    @Mock
    private FollowRepository followRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private TimelineCachePort timelineCachePort;

    @InjectMocks
    private TimelineService timelineService;

    // Test data
    private UUID userId;
    private UUID followee1Id;
    private UUID followee2Id;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        followee1Id = UUID.randomUUID();
        followee2Id = UUID.randomUUID();

        // Default: User exists
        when(userRepo.existsById(userId)).thenReturn(true);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if the requesting user does not exist")
    void getTimeline_UserNotFound() {
        // Arrange
        when(userRepo.existsById(userId)).thenReturn(false); // User does not exist

        // Act & Assert
        UserNotFoundException thrown = assertThrows(
                UserNotFoundException.class,
                () -> timelineService.getTimeline(userId)
        );

        assertEquals("User with ID " + userId + " not found.", thrown.getMessage());
        verify(userRepo, times(1)).existsById(userId); // Only user existence check
        verifyNoInteractions(tweetRepo, followRepo, timelineCachePort); // No other interactions
    }

    @Test
    @DisplayName("Should return cached timeline if available and not empty")
    void getTimeline_CacheHit() {
        // Arrange
        List<Tweet> cachedTweets = List.of(
                new Tweet(UUID.randomUUID(), userId, "Cached tweet 1", LocalDateTime.now()),
                new Tweet(UUID.randomUUID(), followee1Id, "Cached tweet 2", LocalDateTime.now().minusMinutes(5))
        );
        when(timelineCachePort.getTimeline(userId)).thenReturn(Optional.of(cachedTweets));

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(cachedTweets, result); // Ensure the exact cached list is returned

        verify(userRepo, times(1)).existsById(userId); // User existence check
        verify(timelineCachePort, times(1)).getTimeline(userId); // Cache lookup
        // Crucial: Verify no calls to repositories if cache hit
        verifyNoInteractions(tweetRepo, followRepo);
        verify(timelineCachePort, never()).cacheTimeline(any(UUID.class), anyList(), any(Duration.class)); // Should not cache on hit
    }

    @Test
    @DisplayName("Should fetch from DB, sort, limit, and cache if timeline is not in cache (cache miss)")
    void getTimeline_CacheMiss_Success() {
        // Arrange
        // Simulate cache miss
        when(timelineCachePort.getTimeline(userId)).thenReturn(Optional.empty());

        // Simulate followers
        Set<UUID> followees = new HashSet<>(Arrays.asList(followee1Id, followee2Id));
        when(followRepo.findFollowees(userId)).thenReturn(followees);

        // Simulate tweets from user and followees (more than 50 to test limit)
        List<Tweet> allRelevantTweets = new ArrayList<>();
        // 20 tweets from user (recent and old)
        IntStream.range(0, 20).forEach(i -> allRelevantTweets.add(new Tweet(UUID.randomUUID(), userId, "User Tweet " + i, LocalDateTime.now().minusMinutes(i))));
        // 20 tweets from followee1 (recent and old)
        IntStream.range(0, 20).forEach(i -> allRelevantTweets.add(new Tweet(UUID.randomUUID(), followee1Id, "Followee1 Tweet " + i, LocalDateTime.now().minusMinutes(i + 30))));
        // 20 tweets from followee2 (recent and old)
        IntStream.range(0, 20).forEach(i -> allRelevantTweets.add(new Tweet(UUID.randomUUID(), followee2Id, "Followee2 Tweet " + i, LocalDateTime.now().minusMinutes(i + 60))));

        // Shuffle to ensure sorting is tested
        Collections.shuffle(allRelevantTweets, new Random(1));

        // When `findAllByUserIds` is called, return the shuffled list
        when(tweetRepo.findAllByUserIds(anySet())).thenReturn(allRelevantTweets);


        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertEquals(50, result.size()); // Should be limited to 50

        // Verify sorting (most recent first)
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getCreatedAt().isAfter(result.get(i + 1).getCreatedAt()) ||
                            result.get(i).getCreatedAt().isEqual(result.get(i + 1).getCreatedAt()),
                    "Tweets are not sorted correctly: " + result.get(i).getCreatedAt() + " vs " + result.get(i+1).getCreatedAt());
        }

        // Verify repository calls
        verify(userRepo, times(1)).existsById(userId);
        verify(timelineCachePort, times(1)).getTimeline(userId); // Initial cache miss check
        verify(followRepo, times(1)).findFollowees(userId); // Get followees

        // Capture the set of IDs passed to findAllByUserIds
        ArgumentCaptor<Set<UUID>> userIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(tweetRepo, times(1)).findAllByUserIds(userIdsCaptor.capture());
        Set<UUID> capturedUserIds = userIdsCaptor.getValue();
        // Ensure the set includes the user and their followees
        assertTrue(capturedUserIds.contains(userId));
        assertTrue(capturedUserIds.contains(followee1Id));
        assertTrue(capturedUserIds.contains(followee2Id));
        assertEquals(3, capturedUserIds.size()); // User + 2 followees

        // Verify cache storage
        verify(timelineCachePort, times(1)).cacheTimeline(eq(userId), eq(result), eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("Should handle empty followees list and return only user's own tweets")
    void getTimeline_EmptyFollowees() {
        // Arrange
        when(timelineCachePort.getTimeline(userId)).thenReturn(Optional.empty()); // Cache miss
        when(followRepo.findFollowees(userId)).thenReturn(Collections.emptySet()); // No followees

        List<Tweet> userTweets = List.of(
                new Tweet(UUID.randomUUID(), userId, "My tweet 1", LocalDateTime.now()),
                new Tweet(UUID.randomUUID(), userId, "My tweet 2", LocalDateTime.now().minusHours(1))
        );
        when(tweetRepo.findAllByUserIds(anySet())).thenReturn(userTweets);

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(userTweets.get(0).getId(), result.get(0).getId()); // Check sorting
        assertEquals(userTweets.get(1).getId(), result.get(1).getId());

        verify(userRepo, times(1)).existsById(userId);
        verify(timelineCachePort, times(1)).getTimeline(userId);
        verify(followRepo, times(1)).findFollowees(userId);

        ArgumentCaptor<Set<UUID>> userIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(tweetRepo, times(1)).findAllByUserIds(userIdsCaptor.capture());
        Set<UUID> capturedUserIds = userIdsCaptor.getValue();
        // Only the user's ID should be in the set
        assertTrue(capturedUserIds.contains(userId));
        assertEquals(1, capturedUserIds.size());

        verify(timelineCachePort, times(1)).cacheTimeline(eq(userId), eq(result), eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("Should return empty list and not cache if no tweets are found for user or followees")
    void getTimeline_NoTweetsFound() {
        // Arrange
        when(timelineCachePort.getTimeline(userId)).thenReturn(Optional.empty()); // Cache miss
        when(followRepo.findFollowees(userId)).thenReturn(new HashSet<>(Arrays.asList(followee1Id))); // Has followee
        when(tweetRepo.findAllByUserIds(anySet())).thenReturn(Collections.emptyList()); // No tweets found

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Result should be empty

        verify(userRepo, times(1)).existsById(userId);
        verify(timelineCachePort, times(1)).getTimeline(userId);
        verify(followRepo, times(1)).findFollowees(userId);
        verify(tweetRepo, times(1)).findAllByUserIds(anySet());
        verify(timelineCachePort, never()).cacheTimeline(any(UUID.class), anyList(), any(Duration.class)); // Should not cache empty list
    }

    @Test
    @DisplayName("Should return empty list if cached timeline is present but empty")
    void getTimeline_CacheHit_EmptyList() {
        // Arrange
        when(timelineCachePort.getTimeline(userId)).thenReturn(Optional.of(Collections.emptyList()));

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepo, times(1)).existsById(userId);
        verify(timelineCachePort, times(1)).getTimeline(userId);
        verifyNoInteractions(tweetRepo, followRepo); // No repository calls
        verify(timelineCachePort, never()).cacheTimeline(any(UUID.class), anyList(), any(Duration.class)); // No caching if already empty
    }
}