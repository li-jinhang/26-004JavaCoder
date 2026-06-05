package com.example.javacoder.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.javacoder.model.AiCodeReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AiCodeReviewStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsAndFindsReviewBySubmissionId() {
        AiCodeReviewStore store = new AiCodeReviewStore(sqliteJdbcTemplate(), new ObjectMapper());
        AiCodeReview review = store.create(new AiCodeReview(
                0,
                1005,
                123L,
                "alice",
                7,
                "Reverse Words",
                "Good attempt.",
                82,
                "The solution handles the visible cases.",
                "Time complexity is linear.",
                List.of("Missing an edge case."),
                List.of("Add validation for empty input."),
                List.of("Practice two-pointer problems."),
                Instant.parse("2026-06-03T10:15:30Z")
        ));

        AiCodeReviewStore secondStore = new AiCodeReviewStore(sqliteJdbcTemplate(), new ObjectMapper());

        assertThat(review.id()).isEqualTo(3001);
        assertThat(secondStore.findBySubmissionId(1005)).hasValue(review);
    }

    private JdbcTemplate sqliteJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("javacoder-test.sqlite"));
        return new JdbcTemplate(dataSource);
    }
}
