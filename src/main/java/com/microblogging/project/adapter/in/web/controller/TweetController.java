package com.microblogging.project.adapter.in.web.controller;

import com.microblogging.project.application.usecase.PostTweetUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tweets")
public class TweetController {

    private final PostTweetUseCase postTweetUseCase;

    public TweetController(PostTweetUseCase postTweetUseCase) {
        this.postTweetUseCase = postTweetUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> postTweet(@RequestHeader("X-User-Id") UUID userId,
                                          @RequestBody TweetRequest request) {
        postTweetUseCase.post(userId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public record TweetRequest(String content) {}
}

