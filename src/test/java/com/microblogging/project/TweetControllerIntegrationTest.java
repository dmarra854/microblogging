package com.microblogging.project;

import com.microblogging.project.adapter.out.persistence.repository.TweetJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TweetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TweetJpaRepository tweetJpaRepository;

    @Test
    void whenPostTweet_thenSavedInDatabase() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/tweets")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Tweet de integración\"}"))
                .andExpect(status().isCreated());

        var tweets = tweetJpaRepository.findByUserIdIn(java.util.Set.of(userId));
        assertThat(tweets).isNotEmpty();
        assertThat(tweets.get(0).getContent()).isEqualTo("Tweet de integración");
    }
}
