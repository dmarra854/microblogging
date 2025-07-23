package com.microblogging.project.adapter.out.persistence.repository.user;

import com.microblogging.project.adapter.out.persistence.entity.UserEntity; // Import your JPA Entity
import com.microblogging.project.domain.model.User; // Import your domain model
import com.microblogging.project.domain.port.UserRepository; // Import the domain port
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataUserJpaRepository extends JpaRepository<UserEntity, UUID> {
}

@Repository
public class UserJpaRepository implements UserRepository {

    private final SpringDataUserJpaRepository springDataUserJpaRepository;

    public UserJpaRepository(SpringDataUserJpaRepository springDataUserJpaRepository) {
        this.springDataUserJpaRepository = springDataUserJpaRepository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        // Convert UserEntity from repository to User domain model
        return springDataUserJpaRepository.findById(id)
                .map(UserEntity::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = new UserEntity(user);
        UserEntity savedEntity = springDataUserJpaRepository.save(userEntity);
        return savedEntity.toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataUserJpaRepository.existsById(id);
    }
}