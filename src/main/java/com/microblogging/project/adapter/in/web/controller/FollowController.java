package com.microblogging.project.adapter.in.web.controller;

import com.microblogging.project.application.usecase.FollowUserUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/follow")
public class FollowController {

    private final FollowUserUseCase followUserUseCase;

    public FollowController(FollowUserUseCase followUserUseCase) {
        this.followUserUseCase = followUserUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> follow(@RequestHeader("X-User-Id") UUID userId,
                                       @RequestBody FollowRequest request) {
        followUserUseCase.follow(userId, request.followeeId());
        return ResponseEntity.ok().build();
    }

    public record FollowRequest(UUID followeeId) {}
}
