package com.microblogging.project.config;

import com.microblogging.project.domain.port.FollowRepository;
import com.microblogging.project.domain.port.TweetRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

@TestConfiguration
public class TestMocksConfig {

    @Bean
    public FollowRepository followRepository() {
        return Mockito.mock(FollowRepository.class);
    }

    @Bean
    public TweetRepository tweetRepository() {
        return Mockito.mock(TweetRepository.class);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }
}
