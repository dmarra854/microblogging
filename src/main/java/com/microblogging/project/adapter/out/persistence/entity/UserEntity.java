package com.microblogging.project.adapter.out.persistence.entity;

import com.microblogging.project.domain.model.User; // Import domain model for conversion methods
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"users\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserEntity(User user) {
        this(user.getId(), user.getUsername(), String.valueOf(user.hashCode()), user.getEmail(), user.getCreatedAt());
    }

    public User toDomain() {
        return new User(id, username, email, createdAt);
    }
}