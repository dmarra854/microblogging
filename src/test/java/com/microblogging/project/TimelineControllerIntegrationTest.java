package com.microblogging.project;

import com.microblogging.project.adapter.out.persistence.entity.TweetEntity;

import com.microblogging.project.adapter.out.persistence.repository.TweetJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // usa H2
class TimelineControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TweetJpaRepository tweetJpaRepository;

    private UUID userId;
    private UUID followeeId;

    @BeforeEach
    void setUp() {
        tweetJpaRepository.deleteAll();

        userId = UUID.randomUUID();
        followeeId = UUID.randomUUID();

        // Insert tweets for followee
        tweetJpaRepository.save(new TweetEntity(UUID.randomUUID(), followeeId, "Primer tweet", LocalDateTime.now().minusMinutes(10)));
        tweetJpaRepository.save(new TweetEntity(UUID.randomUUID(), followeeId, "Segundo tweet", LocalDateTime.now().minusMinutes(5)));

        // Para simplificar, asumimos que userId sigue a followeeId en la DB de follows (deberías insertar esa info)
        // Pero para test rápido, podés mockear el TimelineQuery si querés
    }

    @Test
    void whenGetTimeline_thenReturnTweets() throws Exception {
        mockMvc.perform(get("/timeline")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content", is("Segundo tweet")))
                .andExpect(jsonPath("$[1].content", is("Primer tweet")));
    }
}
