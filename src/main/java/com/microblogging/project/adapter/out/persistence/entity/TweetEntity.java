package com.microblogging.project.adapter.out.persistence.entity;

import com.microblogging.project.domain.model.Tweet; // Keep this import for conversion methods
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id; // Only @Id needed for pre-generated UUIDs
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString; // NEW: Import ToString

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tweets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString // NEW: Automatically generates a toString() method including all fields.
public class TweetEntity {

    @Id
    @EqualsAndHashCode.Include
    // REMOVED: @GeneratedValue(strategy = GenerationType.XXX)
    // Explanation: Since Tweet.id is generated via UUID.randomUUID() in the domain service
    // (application-side generation), you do NOT use @GeneratedValue here.
    // The ID is already set before the entity is persisted.
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 280)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Existing convenience constructor (good practice for mapping from domain)
    // Lombok's @AllArgsConstructor handles the main constructor for all fields.
    // This specific constructor maps from your domain Tweet object.
    public TweetEntity(Tweet tweet) {
        this(tweet.getId(), tweet.getUserId(), tweet.getContent(), tweet.getCreatedAt());
    }

    // Existing conversion method to domain (good practice for mapping to domain)
    public Tweet toDomain() {
        return new Tweet(id, userId, content, createdAt);
    }
}
