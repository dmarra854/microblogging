package com.microblogging.project.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor // Required for JSON deserialization
@AllArgsConstructor // For easy creation in tests or specific scenarios
@EqualsAndHashCode // For good object comparison, includes all fields by default
@ToString // For helpful logging and debugging
public class FollowRequest {

    @NotNull(message = "Followee ID cannot be null")
    private UUID followeeId;
}