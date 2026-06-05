package com.example.javacoder.service;

import com.example.javacoder.model.TestCaseResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SubmissionStore {

    private static final TypeReference<List<TestCaseResult>> CASE_RESULTS_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicLong submissionId;

    public SubmissionStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        initializeSchema();
        this.submissionId = new AtomicLong(Math.max(1000, currentMaxSubmissionId()));
    }

    public long nextId() {
        return submissionId.incrementAndGet();
    }

    public void save(Submission submission) {
        upsert(submission);
    }

    public void update(Submission submission) {
        upsert(submission);
    }

    public void saveSource(long submissionId, String language, String code) {
        jdbcTemplate.update(
                """
                UPDATE submissions
                SET source_language = ?, source_code = ?
                WHERE id = ?
                """,
                language == null ? "" : language,
                code == null ? "" : code,
                submissionId
        );
    }

    public Optional<Submission> findById(long id) {
        List<Submission> submissions = jdbcTemplate.query(
                """
                SELECT id, user_id, username, problem_id, problem_title, language, status, passed_cases, total_cases,
                       runtime_ms, message, submitted_at, case_results_json
                FROM submissions
                WHERE id = ?
                """,
                (resultSet, rowNumber) -> submissionFromRow(resultSet),
                id
        );
        return submissions.stream().findFirst();
    }

    public Optional<SubmissionSource> findSourceById(long id) {
        List<SubmissionSource> sources = jdbcTemplate.query(
                """
                SELECT id, source_language, source_code
                FROM submissions
                WHERE id = ?
                """,
                (resultSet, rowNumber) -> {
                    String code = resultSet.getString("source_code");
                    if (code == null || code.isBlank()) {
                        return null;
                    }
                    return new SubmissionSource(
                            resultSet.getLong("id"),
                            resultSet.getString("source_language"),
                            code
                    );
                },
                id
        );
        return sources.stream().filter(source -> source != null).findFirst();
    }

    public List<Submission> findRecent() {
        return jdbcTemplate.query(
                """
                SELECT id, user_id, username, problem_id, problem_title, language, status, passed_cases, total_cases,
                       runtime_ms, message, submitted_at, case_results_json
                FROM submissions
                ORDER BY submitted_at DESC
                LIMIT 20
                """,
                (resultSet, rowNumber) -> submissionFromRow(resultSet)
        );
    }

    public List<Submission> findRecentByUserId(long userId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT id, user_id, username, problem_id, problem_title, language, status, passed_cases, total_cases,
                       runtime_ms, message, submitted_at, case_results_json
                FROM submissions
                WHERE user_id = ?
                ORDER BY submitted_at DESC
                LIMIT ?
                """,
                (resultSet, rowNumber) -> submissionFromRow(resultSet),
                userId,
                limit
        );
    }

    public long countByProblemId(long problemId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM submissions WHERE problem_id = ?",
                Long.class,
                problemId
        );
        return count == null ? 0 : count;
    }

    public long acceptedCountByProblemId(long problemId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM submissions WHERE problem_id = ? AND status = ?",
                Long.class,
                problemId,
                "Accepted"
        );
        return count == null ? 0 : count;
    }

    public long countByUserId(long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM submissions WHERE user_id = ?",
                Long.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public long acceptedCountByUserId(long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM submissions WHERE user_id = ? AND status = ?",
                Long.class,
                userId,
                "Accepted"
        );
        return count == null ? 0 : count;
    }

    private void initializeSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS submissions (
                    id INTEGER PRIMARY KEY,
                    user_id INTEGER,
                    username TEXT,
                    problem_id INTEGER NOT NULL,
                    problem_title TEXT NOT NULL,
                    language TEXT NOT NULL,
                    status TEXT NOT NULL,
                    passed_cases INTEGER NOT NULL,
                    total_cases INTEGER NOT NULL,
                    runtime_ms INTEGER NOT NULL,
                    message TEXT NOT NULL,
                    submitted_at TEXT NOT NULL,
                    case_results_json TEXT NOT NULL
                )
                """
        );
        ensureColumn("user_id", "ALTER TABLE submissions ADD COLUMN user_id INTEGER");
        ensureColumn("username", "ALTER TABLE submissions ADD COLUMN username TEXT");
        ensureColumn("source_language", "ALTER TABLE submissions ADD COLUMN source_language TEXT");
        ensureColumn("source_code", "ALTER TABLE submissions ADD COLUMN source_code TEXT");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_submissions_problem_id ON submissions(problem_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_submissions_submitted_at ON submissions(submitted_at)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_submissions_user_id ON submissions(user_id)");
    }

    private long currentMaxSubmissionId() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 1000) FROM submissions", Long.class);
        return maxId == null ? 1000 : maxId;
    }

    private void upsert(Submission submission) {
        jdbcTemplate.update(
                """
                INSERT INTO submissions (
                    id, user_id, username, problem_id, problem_title, language, status, passed_cases, total_cases,
                    runtime_ms, message, submitted_at, case_results_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    user_id = excluded.user_id,
                    username = excluded.username,
                    problem_id = excluded.problem_id,
                    problem_title = excluded.problem_title,
                    language = excluded.language,
                    status = excluded.status,
                    passed_cases = excluded.passed_cases,
                    total_cases = excluded.total_cases,
                    runtime_ms = excluded.runtime_ms,
                    message = excluded.message,
                    submitted_at = excluded.submitted_at,
                    case_results_json = excluded.case_results_json
                """,
                submission.id(),
                submission.userId(),
                submission.username(),
                submission.problemId(),
                submission.problemTitle(),
                submission.language(),
                submission.status(),
                submission.passedCases(),
                submission.totalCases(),
                submission.runtimeMs(),
                submission.message(),
                submission.submittedAt().toString(),
                serializeCaseResults(submission.caseResults())
        );
    }

    private Submission submissionFromRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        long userId = resultSet.getLong("user_id");
        return new Submission(
                resultSet.getLong("id"),
                resultSet.wasNull() ? null : userId,
                resultSet.getString("username"),
                resultSet.getLong("problem_id"),
                resultSet.getString("problem_title"),
                resultSet.getString("language"),
                resultSet.getString("status"),
                resultSet.getInt("passed_cases"),
                resultSet.getInt("total_cases"),
                resultSet.getLong("runtime_ms"),
                resultSet.getString("message"),
                Instant.parse(resultSet.getString("submitted_at")),
                parseCaseResults(resultSet.getString("case_results_json"))
        );
    }

    private void ensureColumn(String columnName, String alterSql) {
        boolean hasColumn = jdbcTemplate.queryForList("PRAGMA table_info(submissions)").stream()
                .map(column -> (String) column.get("name"))
                .anyMatch(columnName::equals);
        if (!hasColumn) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private String serializeCaseResults(List<TestCaseResult> caseResults) {
        try {
            return objectMapper.writeValueAsString(caseResults);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize submission case results.", exception);
        }
    }

    private List<TestCaseResult> parseCaseResults(String caseResultsJson) {
        try {
            return objectMapper.readValue(caseResultsJson, CASE_RESULTS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse submission case results.", exception);
        }
    }
}
