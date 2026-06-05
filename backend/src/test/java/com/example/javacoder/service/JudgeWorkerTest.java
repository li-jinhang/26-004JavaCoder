package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.model.TestCase;
import com.example.javacoder.model.UserRole;
import com.example.javacoder.service.sandbox.JudgeSandboxProperties;
import com.example.javacoder.service.sandbox.JudgeLimits;
import com.example.javacoder.service.sandbox.SandboxProcessResult;
import com.example.javacoder.service.sandbox.SandboxRunner;
import com.example.javacoder.service.sandbox.SandboxSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JudgeWorkerTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsPendingSubmissionAndUpdatesItAfterBackgroundJudging() {
        SubmissionStore submissionStore = new SubmissionStore(sqliteJdbcTemplate(), new ObjectMapper());
        JudgeSandboxProperties properties = new JudgeSandboxProperties();
        LanguageRegistry languageRegistry = new LanguageRegistry(properties);
        JavaJudgeService judgeService = new JavaJudgeService(new AcceptingSandboxRunner(), properties, languageRegistry);
        JudgeWorker judgeWorker = new JudgeWorker(judgeService, submissionStore, languageRegistry);
        Problem problem = new Problem(
                1L,
                "Async",
                "Easy",
                List.of("queue"),
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(new TestCase("", "ok", false))
        );

        Submission pending = judgeWorker.enqueue(problem, new SubmissionRequest(
                1L,
                "java",
                "public class Main { public static void main(String[] args) {} }"
        ), new CurrentUser(101L, "alice", UserRole.USER, Instant.parse("2026-06-03T10:15:30Z")));

        assertThat(pending.status()).isEqualTo("Pending");
        assertThat(pending.userId()).isEqualTo(101L);
        assertThat(pending.username()).isEqualTo("alice");
        assertThat(submissionStore.findById(pending.id())).hasValueSatisfying(
                submission -> assertThat(submission.status()).isIn("Pending", "Judging", "Accepted")
        );

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            Optional<Submission> stored = submissionStore.findById(pending.id());
            assertThat(stored).isPresent();
            assertThat(stored.orElseThrow().status()).isEqualTo("Accepted");
            assertThat(stored.orElseThrow().passedCases()).isEqualTo(1);
            assertThat(stored.orElseThrow().userId()).isEqualTo(101L);
            assertThat(stored.orElseThrow().username()).isEqualTo("alice");
        });

        judgeWorker.close();
    }

    private JdbcTemplate sqliteJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("javacoder-test.sqlite"));
        return new JdbcTemplate(dataSource);
    }

    private static class AcceptingSandboxRunner implements SandboxRunner {

        @Override
        public SandboxSession createSession(com.example.javacoder.service.sandbox.LanguageSpec language, String sourceCode, JudgeLimits limits) {
            return new SandboxSession() {
                @Override
                public SandboxProcessResult compile() {
                    return new SandboxProcessResult(0, "", "", false, false, false, 10);
                }

                @Override
                public SandboxProcessResult run(String input) {
                    return new SandboxProcessResult(0, "ok", "", false, false, false, 20);
                }

                @Override
                public void close() {
                }
            };
        }
    }
}
