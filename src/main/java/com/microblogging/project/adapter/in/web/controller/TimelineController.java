package com.microblogging.project.adapter.in.web.controller;

import com.microblogging.project.application.usecase.TimelineQuery;
import com.microblogging.project.domain.model.Tweet;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/timeline")
public class TimelineController {

    private final TimelineQuery timelineQuery;

    public TimelineController(TimelineQuery timelineQuery) {
        this.timelineQuery = timelineQuery;
    }

    @GetMapping
    public ResponseEntity<List<TweetDTO>> timeline(@RequestHeader("X-User-Id") UUID userId) {
        List<Tweet> tweets = timelineQuery.getTimeline(userId);
        List<TweetDTO> response = tweets.stream()
                .map(t -> new TweetDTO(t.getUserId(), t.getContent(), t.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(response);
    }

    public record TweetDTO(UUID userId, String content, LocalDateTime createdAt) {}
}