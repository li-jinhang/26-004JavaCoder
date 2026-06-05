package com.example.javacoder.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.TestCase;
import com.example.javacoder.model.TestCaseResult;
import com.example.javacoder.model.UserRole;
import com.example.javacoder.repository.ProblemRepository;
import com.example.javacoder.service.SubmissionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AiCodeReviewServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createsReviewFromJudgedSubmissionAndCachesIt() {
        ObjectMapper objectMapper = new ObjectMapper();
        SubmissionStore submissionStore = new SubmissionStore(sqliteJdbcTemplate(), objectMapper);
        AiCodeReviewStore reviewStore = new AiCodeReviewStore(sqliteJdbcTemplate(), objectMapper);
        ProblemRepository problemRepository = problemRepository(objectMapper);
        LlmProperties properties = new LlmProperties();
        AiCodeReviewService service = new AiCodeReviewService(
                submissionStore,
                problemRepository,
                reviewStore,
                new AiCodeReviewPromptBuilder(properties),
                new FakeLlmClient("""
                        {
                          "summary": "Clear and correct.",
                          "score": 95,
                          "correctness": "It passes the judge result.",
                          "complexity": "The program is constant time for this task.",
                          "bugs": [],
                          "improvements": ["Keep the input parsing explicit."],
                          "nextSteps": ["Try a harder I/O problem."]
                        }
                        """),
                objectMapper
        );
        Submission submission = new Submission(
                1005,
                123L,
                "alice",
                1,
                "Echo",
                "Java 17",
                "Accepted",
                1,
                1,
                20,
                "All test cases passed.",
                Instant.parse("2026-06-03T10:15:30Z"),
                List.of(new TestCaseResult(1, false, "Accepted", "hi", "hi", "hi", 20, "ok"))
        );
        submissionStore.save(submission);
        submissionStore.saveSource(1005, "java", "public class Main {}");

        var review = service.reviewSubmission(
                1005,
                new CurrentUser(123L, "alice", UserRole.USER, Instant.parse("2026-06-03T10:15:30Z"))
        );
        var cached = service.reviewSubmission(
                1005,
                new CurrentUser(123L, "alice", UserRole.USER, Instant.parse("2026-06-03T10:15:30Z"))
        );

        assertThat(review.summary()).isEqualTo("Clear and correct.");
        assertThat(review.score()).isEqualTo(95);
        assertThat(cached).isEqualTo(review);
    }

    @Test
    void acceptsJsonWrappedInMarkdownFence() {
        ObjectMapper objectMapper = new ObjectMapper();
        SubmissionStore submissionStore = new SubmissionStore(sqliteJdbcTemplate(), objectMapper);
        AiCodeReviewStore reviewStore = new AiCodeReviewStore(sqliteJdbcTemplate(), objectMapper);
        ProblemRepository problemRepository = problemRepository(objectMapper);
        LlmProperties properties = new LlmProperties();
        AiCodeReviewService service = new AiCodeReviewService(
                submissionStore,
                problemRepository,
                reviewStore,
                new AiCodeReviewPromptBuilder(properties),
                new FakeLlmClient("""
                        Here is the review:
                        ```json
                        {
                          "summary": "Wrapped but valid.",
                          "score": 88,
                          "correctness": "The logic matches the visible result.",
                          "complexity": "The solution is efficient.",
                          "bugs": [],
                          "improvements": ["Use clearer variable names."],
                          "nextSteps": ["Try another problem."]
                        }
                        ```
                        """),
                objectMapper
        );
        Submission submission = new Submission(
                1006,
                123L,
                "alice",
                1,
                "Echo",
                "Java 17",
                "Accepted",
                1,
                1,
                20,
                "All test cases passed.",
                Instant.parse("2026-06-03T10:15:30Z"),
                List.of(new TestCaseResult(1, false, "Accepted", "hi", "hi", "hi", 20, "ok"))
        );
        submissionStore.save(submission);
        submissionStore.saveSource(1006, "java", "public class Main {}");

        var review = service.reviewSubmission(
                1006,
                new CurrentUser(123L, "alice", UserRole.USER, Instant.parse("2026-06-03T10:15:30Z"))
        );

        assertThat(review.summary()).isEqualTo("Wrapped but valid.");
        assertThat(review.score()).isEqualTo(88);
    }

    private ProblemRepository problemRepository(ObjectMapper objectMapper) {
        ResourcePatternResolver resolver = new ResourcePatternResolver() {
            @Override
            public Resource getResource(String location) {
                return resource();
            }

            @Override
            public ClassLoader getClassLoader() {
                return getClass().getClassLoader();
            }

            @Override
            public Resource[] getResources(String locationPattern) {
                return new Resource[] { resource() };
            }
        };
        return new ProblemRepository(resolver, objectMapper);
    }

    private Resource resource() {
        return new AbstractResource() {
            @Override
            public String getDescription() {
                return "test-problem";
            }

            @Override
            public java.io.InputStream getInputStream() {
                String json = """
                        {
                          "id": 1,
                          "title": "Echo",
                          "difficulty": "Easy",
                          "tags": ["io"],
                          "description": "Echo one token.",
                          "inputFormat": "One token.",
                          "outputFormat": "The same token.",
                          "constraints": "1 <= length <= 20",
                          "starterCode": "",
                          "examples": [],
                          "testCases": [
                            {
                              "input": "hi",
                              "expectedOutput": "hi",
                              "hidden": false
                            }
                          ],
                          "referenceSolution": ""
                        }
                        """;
                return new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        };
    }

    private JdbcTemplate sqliteJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("javacoder-test.sqlite"));
        return new JdbcTemplate(dataSource);
    }

    private static class FakeLlmClient implements LlmClient {

        private final String response;

        FakeLlmClient(String response) {
            this.response = response;
        }

        @Override
        public String completeJson(String systemPrompt, String userPrompt) {
            assertThat(systemPrompt).contains("Simplified Chinese");
            assertThat(userPrompt).contains("public class Main {}");
            assertThat(userPrompt).doesNotContain("Hidden input");
            return response;
        }
    }
}
