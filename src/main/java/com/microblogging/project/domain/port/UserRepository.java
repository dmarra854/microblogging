package com.microblogging.project.domain.port;

import com.microblogging.project.domain.model.User; // Import domain model
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    User save(User user);
    boolean existsById(UUID id);
}