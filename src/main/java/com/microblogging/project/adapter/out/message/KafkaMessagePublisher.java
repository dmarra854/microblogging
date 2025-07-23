package com.microblogging.project.adapter.out.message;

import com.microblogging.project.domain.event.TweetPostedEvent;
import com.microblogging.project.domain.port.MessagePublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class KafkaMessagePublisher implements MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaMessagePublisher.class);
    private final KafkaTemplate<String, TweetPostedEvent> kafkaTemplate;
    private static final String TWEET_EVENTS_TOPIC = "tweet-posted-events";

    public KafkaMessagePublisher(KafkaTemplate<String, TweetPostedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishTweetPostedEvent(TweetPostedEvent event) {
        kafkaTemplate.send(TWEET_EVENTS_TOPIC, event.tweetId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published TweetPostedEvent for tweet {} to topic {}. Offset: {}",
                                event.tweetId(), TWEET_EVENTS_TOPIC, result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish TweetPostedEvent for tweet {}. Error: {}",
                                event.tweetId(), ex.getMessage(), ex);
                    }
                });
    }
}