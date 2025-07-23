package com.microblogging.project;

import com.microblogging.project.application.service.TimelineService;
import com.microblogging.project.domain.exception.UserNotFoundException;
import com.microblogging.project.domain.model.Tweet;
import com.microblogging.project.domain.port.FollowRepository;
import com.microblogging.project.domain.port.TweetRepository;
import com.microblogging.project.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private UserRepository userRepo; // Mock the new UserRepository dependency

    @Mock
    private RedisTemplate<String, Tweet> redisTemplate;

    @Mock // Mock ListOperations specifically for RedisTemplate interactions
    private ListOperations<String, Tweet> listOperations;

    @InjectMocks
    private TimelineService timelineService;

    // Test data
    private UUID userId;
    private UUID followee1Id;
    private UUID followee2Id;
    private UUID nonExistentUserId;
    private String redisKey;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        followee1Id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        followee2Id = UUID.fromString("00000000-0000-0000-0000-000000000003");
        nonExistentUserId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        redisKey = "timeline:" + userId;

        // Common RedisTemplate mock setup for list operations
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        // Common UserRepository mock setup
        when(userRepo.existsById(userId)).thenReturn(true);
        when(userRepo.existsById(nonExistentUserId)).thenReturn(false);
    }

    // Helper to create a list of tweets
    private List<Tweet> createTweets(UUID ownerId, int count, LocalDateTime startDateTime) {
        return IntStream.range(0, count)
                .mapToObj(i -> new Tweet(UUID.randomUUID(), ownerId, "Content " + i, startDateTime.minusMinutes(i)))
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if user does not exist")
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        // Arrange
        when(userRepo.existsById(nonExistentUserId)).thenReturn(false);

        // Act & Assert
        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            timelineService.getTimeline(nonExistentUserId);
        });

        assertEquals("User with ID " + nonExistentUserId + " not found.", thrown.getMessage());
        verify(userRepo, times(1)).existsById(nonExistentUserId);
        // Verify no other interactions if user not found
        verifyNoInteractions(listOperations, followRepo, tweetRepo);
    }

    @Test
    @DisplayName("Should return cached tweets from Redis on cache hit")
    void shouldReturnCachedTweetsOnCacheHit() {
        // Arrange
        List<Tweet> cachedTweets = createTweets(userId, 10, LocalDateTime.now());
        when(listOperations.range(redisKey, 0, 49)).thenReturn(cachedTweets);

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertEquals(cachedTweets.size(), result.size());
        assertEquals(cachedTweets, result);

        // Verify Redis cache was checked
        verify(listOperations, times(1)).range(redisKey, 0, 49);
        // Verify no database interactions occurred
        verifyNoInteractions(followRepo, tweetRepo);
        // Verify no write to cache occurred
        verify(listOperations, never()).rightPushAll(anyString(), anyList());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Should fetch from database and cache in Redis on cache miss")
    void shouldFetchFromDatabaseAndCacheOnCacheMiss() {
        // Arrange
        // Simulate cache miss
        when(listOperations.range(redisKey, 0, 49)).thenReturn(Collections.emptyList());

        Set<UUID> followees = new HashSet<>(Arrays.asList(followee1Id, followee2Id));
        when(followRepo.findFollowees(userId)).thenReturn(followees);

        // Prepare tweets from database, including user's own tweets
        Tweet userTweet1 = new Tweet(UUID.randomUUID(), userId, "My tweet 1", LocalDateTime.now().minusMinutes(1));
        Tweet userTweet2 = new Tweet(UUID.randomUUID(), userId, "My tweet 2", LocalDateTime.now().minusMinutes(5));
        Tweet followee1Tweet = new Tweet(UUID.randomUUID(), followee1Id, "F1 tweet", LocalDateTime.now().minusMinutes(2));
        Tweet followee2Tweet = new Tweet(UUID.randomUUID(), followee2Id, "F2 tweet", LocalDateTime.now().minusMinutes(3));

        List<Tweet> allTweetsFromDb = Arrays.asList(
                userTweet1, followee1Tweet, followee2Tweet, userTweet2
        );
        // Ensure tweetRepo returns all relevant tweets, then the service will filter/sort
        when(tweetRepo.findAllByUserIds(anySet())).thenReturn(allTweetsFromDb);

        // Mock Redis write operations
        doNothing().when(listOperations).rightPushAll(eq(redisKey), anyList());
        doNothing().when(redisTemplate).expire(eq(redisKey), any(Duration.class));

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size()); // All 4 tweets should be returned if limit is 50
        // Verify sorting (most recent first)
        assertEquals(userTweet1, result.get(0));
        assertEquals(followee1Tweet, result.get(1));
        assertEquals(followee2Tweet, result.get(2));
        assertEquals(userTweet2, result.get(3));

        // Verify cache read attempt
        verify(listOperations, times(1)).range(redisKey, 0, 49);
        // Verify database interactions
        verify(followRepo, times(1)).findFollowees(userId);
        // Verify findAllByUserIds was called with a set that includes original user ID + followees
        Set<UUID> expectedUserIdsForDbQuery = new HashSet<>(Arrays.asList(userId, followee1Id, followee2Id));
        verify(tweetRepo, times(1)).findAllByUserIds(argThat(set -> set.containsAll(expectedUserIdsForDbQuery) && expectedUserIdsForDbQuery.containsAll(set)));
        // Verify cache write occurred
        verify(listOperations, times(1)).rightPushAll(eq(redisKey), anyList());
        verify(redisTemplate, times(1)).expire(eq(redisKey), any(Duration.class));
    }

    @Test
    @DisplayName("Should fetch from database if Redis read fails")
    void shouldFetchFromDatabaseWhenRedisReadFails() {
        // Arrange
        // Simulate Redis read failure
        when(listOperations.range(redisKey, 0, 49)).thenThrow(new RuntimeException("Redis connection lost during read"));

        Set<UUID> followees = new HashSet<>(Arrays.asList(followee1Id));
        when(followRepo.findFollowees(userId)).thenReturn(followees);

        Tweet userTweet = new Tweet(UUID.randomUUID(), userId, "My tweet", LocalDateTime.now().minusMinutes(1));
        Tweet followeeTweet = new Tweet(UUID.randomUUID(), followee1Id, "F1 tweet", LocalDateTime.now().minusMinutes(2));
        List<Tweet> dbTweets = Arrays.asList(userTweet, followeeTweet);

        Set<UUID> expectedDbQueryIds = new HashSet<>(Arrays.asList(userId, followee1Id));
        when(tweetRepo.findAllByUserIds(argThat(set -> set.containsAll(expectedDbQueryIds) && expectedDbQueryIds.containsAll(set)))).thenReturn(dbTweets);

        // Mock Redis write operations (might still be attempted and fail)
        doThrow(new RuntimeException("Redis connection lost during write")).when(listOperations).rightPushAll(eq(redisKey), anyList());

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(userTweet, result.get(0));
        assertEquals(followeeTweet, result.get(1));

        // Verify Redis read attempt and failure
        verify(listOperations, times(1)).range(redisKey, 0, 49);
        // Verify database interactions occurred as fallback
        verify(followRepo, times(1)).findFollowees(userId);
        verify(tweetRepo, times(1)).findAllByUserIds(anySet());
        // Verify Redis write attempt (even if it failed internally)
        verify(listOperations, times(1)).rightPushAll(eq(redisKey), anyList());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class)); // Expire won't be called if rightPushAll fails
    }

    @Test
    @DisplayName("Should return tweets from database even if Redis write fails")
    void shouldReturnTweetsWhenRedisWriteFails() {
        // Arrange
        // Simulate cache miss
        when(listOperations.range(redisKey, 0, 49)).thenReturn(Collections.emptyList());

        Set<UUID> followees = new HashSet<>(Arrays.asList(followee1Id));
        when(followRepo.findFollowees(userId)).thenReturn(followees);

        Tweet userTweet = new Tweet(UUID.randomUUID(), userId, "My tweet", LocalDateTime.now());
        List<Tweet> dbTweets = Collections.singletonList(userTweet);
        when(tweetRepo.findAllByUserIds(anySet())).thenReturn(dbTweets);

        // Simulate Redis write failure
        doThrow(new RuntimeException("Redis connection lost during write")).when(listOperations).rightPushAll(eq(redisKey), anyList());

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userTweet, result.get(0));

        // Verify cache read attempt
        verify(listOperations, times(1)).range(redisKey, 0, 49);
        // Verify database interactions
        verify(followRepo, times(1)).findFollowees(userId);
        verify(tweetRepo, times(1)).findAllByUserIds(anySet());
        // Verify Redis write attempt occurred (and failed)
        verify(listOperations, times(1)).rightPushAll(eq(redisKey), anyList());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class)); // Expire won't be called if rightPushAll fails
    }

    @Test
    @DisplayName("Should return only user's own tweets if no followees")
    void shouldReturnOnlyOwnTweetsIfNoFollowees() {
        // Arrange
        // Simulate cache miss
        when(listOperations.range(redisKey, 0, 49)).thenReturn(Collections.emptyList());

        // Simulate no followees
        when(followRepo.findFollowees(userId)).thenReturn(Collections.emptySet());

        // Prepare only user's own tweets
        Tweet userTweet1 = new Tweet(UUID.randomUUID(), userId, "My tweet 1", LocalDateTime.now().minusMinutes(1));
        Tweet userTweet2 = new Tweet(UUID.randomUUID(), userId, "My tweet 2", LocalDateTime.now().minusMinutes(5));

        List<Tweet> ownTweets = Arrays.asList(userTweet1, userTweet2);
        // Ensure tweetRepo returns only user's tweets when queried with just user ID
        when(tweetRepo.findAllByUserIds(eq(new HashSet<>(Collections.singletonList(userId)))))
                .thenReturn(ownTweets);

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(ownTweets)); // Ensure both tweets are there
        assertEquals(userTweet1, result.get(0)); // Check sorting
        assertEquals(userTweet2, result.get(1));

        verify(listOperations, times(1)).range(redisKey, 0, 49);
        verify(followRepo, times(1)).findFollowees(userId);
        verify(tweetRepo, times(1)).findAllByUserIds(anySet()); // Verify it was called with the correct set containing only userId
        verify(listOperations, times(1)).rightPushAll(eq(redisKey), anyList());
        verify(redisTemplate, times(1)).expire(eq(redisKey), any(Duration.class));
    }

    @Test
    @DisplayName("Should return sorted and limited tweets (up to 50)")
    void shouldReturnSortedAndLimitedTweets() {
        // Arrange
        when(listOperations.range(redisKey, 0, 49)).thenReturn(Collections.emptyList());

        Set<UUID> followees = new HashSet<>(Arrays.asList(followee1Id));
        when(followRepo.findFollowees(userId)).thenReturn(followees);

        // Create more than 50 tweets, out of order, from various users
        List<Tweet> rawTweets = IntStream.range(0, 60)
                .mapToObj(i -> {
                    UUID owner = (i % 3 == 0) ? userId : (i % 3 == 1 ? followee1Id : followee2Id);
                    return new Tweet(UUID.randomUUID(), owner, "Test " + i, LocalDateTime.now().minusSeconds(60 - i));
                })
                .collect(Collectors.toList());

        when(tweetRepo.findAllByUserIds(anySet())).thenReturn(rawTweets);

        // Act
        List<Tweet> result = timelineService.getTimeline(userId);

        // Assert
        assertNotNull(result);
        assertEquals(50, result.size()); // Should be limited to 50

        // Verify sorting (most recent first)
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getCreatedAt().isAfter(result.get(i + 1).getCreatedAt()) ||
                    result.get(i).getCreatedAt().isEqual(result.get(i + 1).getCreatedAt()));
        }

        verify(listOperations, times(1)).range(redisKey, 0, 49);
        verify(followRepo, times(1)).findFollowees(userId);
        verify(tweetRepo, times(1)).findAllByUserIds(anySet());
        verify(listOperations, times(1)).rightPushAll(eq(redisKey), anyList());
        verify(redisTemplate, times(1)).expire(eq(redisKey), any(Duration.class));
    }
}