package com.microblogging.project.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
// Domain Layer - Tweet.java
public class Tweet {
    private final UUID id;
    private final UUID userId;
    private final String content;
    private final LocalDateTime createdAt;
}
