package com.microblogging.project.domain.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

// Simple User domain model
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // For convenience
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class User {
    @EqualsAndHashCode.Include
    private UUID id;
    private String username;
    private String email;
    private LocalDateTime createdAt;

    // You could add validation here if username/email has specific rules
}