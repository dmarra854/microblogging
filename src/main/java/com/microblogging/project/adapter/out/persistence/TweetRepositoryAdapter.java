package com.microblogging.project.adapter.out.persistence;

import com.microblogging.project.adapter.out.persistence.entity.TweetEntity;
import com.microblogging.project.adapter.out.persistence.repository.TweetJpaRepository;
import com.microblogging.project.domain.model.Tweet;
import com.microblogging.project.domain.port.TweetRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class TweetRepositoryAdapter implements TweetRepository {

    private final TweetJpaRepository jpaRepository;

    public TweetRepositoryAdapter(TweetJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Tweet tweet) {
        TweetEntity entity = new TweetEntity(
                tweet.getId(),
                tweet.getUserId(),
                tweet.getContent(),
                tweet.getCreatedAt()
        );
        jpaRepository.save(entity);
    }

    @Override
    public List<Tweet> findAllByUserIds(Set<UUID> userIds) {
        return jpaRepository.findByUserIdIn(userIds).stream()
                .map(entity -> new Tweet(
                        entity.getId(),
                        entity.getUserId(),
                        entity.getContent(),
                        entity.getCreatedAt()
                ))
                .toList();
    }
}
