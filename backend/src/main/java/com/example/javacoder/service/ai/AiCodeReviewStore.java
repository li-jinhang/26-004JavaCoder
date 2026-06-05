package com.example.javacoder.service.ai;

import com.example.javacoder.model.AiCodeReview;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiCodeReviewStore {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicLong reviewId;

    public AiCodeReviewStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        initializeSchema();
        this.reviewId = new AtomicLong(Math.max(3000, currentMaxReviewId()));
    }

    public AiCodeReview create(AiCodeReview review) {
        AiCodeReview persisted = new AiCodeReview(
                reviewId.incrementAndGet(),
                review.submissionId(),
                review.userId(),
                review.username(),
                review.problemId(),
                review.problemTitle(),
                review.summary(),
                review.score(),
                review.correctness(),
                review.complexity(),
                review.bugs(),
                review.improvements(),
                review.nextSteps(),
                review.createdAt()
        );
        insert(persisted);
        return persisted;
    }

    public Optional<AiCodeReview> findBySubmissionId(long submissionId) {
        List<AiCodeReview> reviews = jdbcTemplate.query(
                """
                SELECT id, submission_id, user_id, username, problem_id, problem_title, summary, score,
                       correctness, complexity, bugs_json, improvements_json, next_steps_json, created_at
                FROM ai_code_reviews
                WHERE submission_id = ?
                """,
                this::mapReview,
                submissionId
        );
        return reviews.stream().findFirst();
    }

    private void initializeSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS ai_code_reviews (
                    id INTEGER PRIMARY KEY,
                    submission_id INTEGER NOT NULL UNIQUE,
                    user_id INTEGER,
                    username TEXT,
                    problem_id INTEGER NOT NULL,
                    problem_title TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    score INTEGER NOT NULL,
                    correctness TEXT NOT NULL,
                    complexity TEXT NOT NULL,
                    bugs_json TEXT NOT NULL,
                    improvements_json TEXT NOT NULL,
                    next_steps_json TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
                """
        );
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_code_reviews_submission_id ON ai_code_reviews(submission_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_code_reviews_user_id ON ai_code_reviews(user_id)");
    }

    private long currentMaxReviewId() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 3000) FROM ai_code_reviews", Long.class);
        return maxId == null ? 3000 : maxId;
    }

    private void insert(AiCodeReview review) {
        jdbcTemplate.update(
                """
                INSERT INTO ai_code_reviews (
                    id, submission_id, user_id, username, problem_id, problem_title, summary, score,
                    correctness, complexity, bugs_json, improvements_json, next_steps_json, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(submission_id) DO UPDATE SET
                    summary = excluded.summary,
                    score = excluded.score,
                    correctness = excluded.correctness,
                    complexity = excluded.complexity,
                    bugs_json = excluded.bugs_json,
                    improvements_json = excluded.improvements_json,
                    next_steps_json = excluded.next_steps_json,
                    created_at = excluded.created_at
                """,
                review.id(),
                review.submissionId(),
                review.userId(),
                review.username(),
                review.problemId(),
                review.problemTitle(),
                review.summary(),
                review.score(),
                review.correctness(),
                review.complexity(),
                serializeList(review.bugs()),
                serializeList(review.improvements()),
                serializeList(review.nextSteps()),
                review.createdAt().toString()
        );
    }

    private AiCodeReview mapReview(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        long userId = resultSet.getLong("user_id");
        return new AiCodeReview(
                resultSet.getLong("id"),
                resultSet.getLong("submission_id"),
                resultSet.wasNull() ? null : userId,
                resultSet.getString("username"),
                resultSet.getLong("problem_id"),
                resultSet.getString("problem_title"),
                resultSet.getString("summary"),
                resultSet.getInt("score"),
                resultSet.getString("correctness"),
                resultSet.getString("complexity"),
                parseList(resultSet.getString("bugs_json")),
                parseList(resultSet.getString("improvements_json")),
                parseList(resultSet.getString("next_steps_json")),
                Instant.parse(resultSet.getString("created_at"))
        );
    }

    private String serializeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize AI review list.", exception);
        }
    }

    private List<String> parseList(String value) {
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse AI review list.", exception);
        }
    }
}
