package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.javacoder.model.Submission;
import com.example.javacoder.model.TestCaseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SubmissionStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsSubmissionsAndRestoresNextIdAcrossStoreInstances() {
        SubmissionStore firstStore = new SubmissionStore(sqliteJdbcTemplate(), new ObjectMapper());
        Submission submission = new Submission(
                1005,
                123L,
                "alice",
                7,
                "Reverse Words",
                "Java",
                "Accepted",
                1,
                1,
                42,
                "ok",
                Instant.parse("2026-06-03T10:15:30Z"),
                List.of(new TestCaseResult(1, false, "Accepted", "a b", "b a", "b a", 42, "ok"))
        );

        firstStore.save(submission);
        firstStore.saveSource(1005, "java", "public class Main {}");

        SubmissionStore secondStore = new SubmissionStore(sqliteJdbcTemplate(), new ObjectMapper());

        assertThat(secondStore.findById(1005)).hasValue(submission);
        assertThat(secondStore.findSourceById(1005)).hasValueSatisfying(source -> {
            assertThat(source.language()).isEqualTo("java");
            assertThat(source.code()).isEqualTo("public class Main {}");
        });
        assertThat(secondStore.findRecent()).containsExactly(submission);
        assertThat(secondStore.countByProblemId(7)).isEqualTo(1);
        assertThat(secondStore.acceptedCountByProblemId(7)).isEqualTo(1);
        assertThat(secondStore.countByUserId(123)).isEqualTo(1);
        assertThat(secondStore.acceptedCountByUserId(123)).isEqualTo(1);
        assertThat(secondStore.findRecentByUserId(123, 5)).containsExactly(submission);
        assertThat(secondStore.nextId()).isEqualTo(1006);
    }

    private JdbcTemplate sqliteJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("javacoder-test.sqlite"));
        return new JdbcTemplate(dataSource);
    }
}
