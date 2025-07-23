package com.microblogging.project.adapter.out.persistence.repository;

import com.microblogging.project.adapter.out.persistence.entity.FollowEntity; // Import your Lombok-ified entity
import com.microblogging.project.domain.port.FollowRepository; // Import the domain port
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

interface SpringDataFollowJpaRepository extends JpaRepository<FollowEntity, Long> {
    Optional<FollowEntity> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    void deleteByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    @Query("SELECT fe.followerId FROM FollowEntity fe WHERE fe.followeeId = :followeeId")
    Set<UUID> findFollowerIdsByFolloweeId(@Param("followeeId") UUID followeeId);

    @Query("SELECT fe.followeeId FROM FollowEntity fe WHERE fe.followerId = :followerId")
    Set<UUID> findFolloweeIdsByFollowerId(@Param("followerId") UUID followerId);
}


@Repository
public class FollowJpaRepository implements FollowRepository {

    private final SpringDataFollowJpaRepository springDataFollowJpaRepository;

    public FollowJpaRepository(SpringDataFollowJpaRepository springDataFollowJpaRepository) {
        this.springDataFollowJpaRepository = springDataFollowJpaRepository;
    }

    @Override
    public void save(UUID followerId, UUID followeeId) {
        FollowEntity followEntity = new FollowEntity(null, followerId, followeeId); // id is auto-generated
        springDataFollowJpaRepository.save(followEntity);
    }

    @Override
    public void delete(UUID followerId, UUID followeeId) {
        springDataFollowJpaRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Override
    public boolean exists(UUID followerId, UUID followeeId) {
        return springDataFollowJpaRepository.findByFollowerIdAndFolloweeId(followerId, followeeId).isPresent();
    }

    @Override
    public Set<UUID> findFollowersByFolloweeId(UUID followeeId) {
        return springDataFollowJpaRepository.findFollowerIdsByFolloweeId(followeeId);
    }

    @Override
    public Set<UUID> findFollowees(UUID followerId) {
        return springDataFollowJpaRepository.findFolloweeIdsByFollowerId(followerId);
    }
}