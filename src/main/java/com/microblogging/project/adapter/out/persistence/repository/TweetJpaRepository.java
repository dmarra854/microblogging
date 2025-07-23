package com.microblogging.project.adapter.out.persistence.repository;

import com.microblogging.project.adapter.out.persistence.entity.TweetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TweetJpaRepository extends JpaRepository<TweetEntity, UUID> {
    List<TweetEntity> findByUserIdIn(Set<UUID> userIds);
}