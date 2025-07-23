package com.microblogging.project.application.usecase;

import com.microblogging.project.domain.model.Tweet;

import java.util.List;
import java.util.UUID;

public interface TimelineQuery {
    List<Tweet> getTimeline(UUID userId);
}
