package com.example.javacoder.service;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Solution;
import com.example.javacoder.model.SolutionRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SolutionStore {

    private static final String PUBLISHED = "PUBLISHED";

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong solutionId;

    public SolutionStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeSchema();
        this.solutionId = new AtomicLong(Math.max(2000, currentMaxSolutionId()));
    }

    public Solution create(Problem problem, CurrentUser author, SolutionRequest request) {
        NormalizedSolutionInput input = normalize(request);
        Instant now = Instant.now();
        Solution solution = new Solution(
                solutionId.incrementAndGet(),
                problem.id(),
                problem.title(),
                author.id(),
                author.username(),
                input.title(),
                input.content(),
                input.language(),
                input.code(),
                input.relatedSubmissionId(),
                PUBLISHED,
                now,
                now
        );
        insert(solution);
        return solution;
    }

    public Optional<Solution> findById(long id) {
        List<Solution> solutions = jdbcTemplate.query(
                """
                SELECT id, problem_id, problem_title, author_id, author_name, title, content, language,
                       code, related_submission_id, status, created_at, updated_at
                FROM solutions
                WHERE id = ? AND status = ?
                """,
                this::mapSolution,
                id,
                PUBLISHED
        );
        return solutions.stream().findFirst();
    }

    public List<Solution> findByProblemId(long problemId) {
        return jdbcTemplate.query(
                """
                SELECT id, problem_id, problem_title, author_id, author_name, title, content, language,
                       code, related_submission_id, status, created_at, updated_at
                FROM solutions
                WHERE problem_id = ? AND status = ?
                ORDER BY updated_at DESC, id DESC
                """,
                this::mapSolution,
                problemId,
                PUBLISHED
        );
    }

    public Solution update(long id, SolutionRequest request) {
        Solution existing = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("题解不存在。"));
        NormalizedSolutionInput input = normalize(request);
        Solution updated = new Solution(
                existing.id(),
                existing.problemId(),
                existing.problemTitle(),
                existing.authorId(),
                existing.authorName(),
                input.title(),
                input.content(),
                input.language(),
                input.code(),
                input.relatedSubmissionId(),
                existing.status(),
                existing.createdAt(),
                Instant.now()
        );
        jdbcTemplate.update(
                """
                UPDATE solutions
                SET title = ?, content = ?, language = ?, code = ?, related_submission_id = ?, updated_at = ?
                WHERE id = ? AND status = ?
                """,
                updated.title(),
                updated.content(),
                updated.language(),
                updated.code(),
                updated.relatedSubmissionId(),
                updated.updatedAt().toString(),
                updated.id(),
                PUBLISHED
        );
        return updated;
    }

    public boolean delete(long id) {
        return jdbcTemplate.update(
                "UPDATE solutions SET status = ?, updated_at = ? WHERE id = ? AND status = ?",
                "DELETED",
                Instant.now().toString(),
                id,
                PUBLISHED
        ) > 0;
    }

    private void initializeSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS solutions (
                    id INTEGER PRIMARY KEY,
                    problem_id INTEGER NOT NULL,
                    problem_title TEXT NOT NULL,
                    author_id INTEGER NOT NULL,
                    author_name TEXT NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    language TEXT NOT NULL,
                    code TEXT NOT NULL,
                    related_submission_id INTEGER,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """
        );
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_solutions_problem_id ON solutions(problem_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_solutions_author_id ON solutions(author_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_solutions_updated_at ON solutions(updated_at)");
    }

    private long currentMaxSolutionId() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 2000) FROM solutions", Long.class);
        return maxId == null ? 2000 : maxId;
    }

    private void insert(Solution solution) {
        jdbcTemplate.update(
                """
                INSERT INTO solutions (
                    id, problem_id, problem_title, author_id, author_name, title, content, language,
                    code, related_submission_id, status, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                solution.id(),
                solution.problemId(),
                solution.problemTitle(),
                solution.authorId(),
                solution.authorName(),
                solution.title(),
                solution.content(),
                solution.language(),
                solution.code(),
                solution.relatedSubmissionId(),
                solution.status(),
                solution.createdAt().toString(),
                solution.updatedAt().toString()
        );
    }

    private Solution mapSolution(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        long relatedSubmissionId = resultSet.getLong("related_submission_id");
        return new Solution(
                resultSet.getLong("id"),
                resultSet.getLong("problem_id"),
                resultSet.getString("problem_title"),
                resultSet.getLong("author_id"),
                resultSet.getString("author_name"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getString("language"),
                resultSet.getString("code"),
                resultSet.wasNull() ? null : relatedSubmissionId,
                resultSet.getString("status"),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }

    private NormalizedSolutionInput normalize(SolutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请输入题解内容。");
        }

        String title = normalizeRequiredText(request.title(), 3, 80, "题解标题");
        String content = normalizeRequiredText(request.content(), 20, 20_000, "题解正文");
        String language = normalizeLanguage(request.language());
        String code = request.code() == null ? "" : request.code().trim();
        if (code.length() > 65_536) {
            throw new IllegalArgumentException("题解代码不能超过 65536 个字符。");
        }
        return new NormalizedSolutionInput(title, content, language, code, request.relatedSubmissionId());
    }

    private String normalizeRequiredText(String value, int minLength, int maxLength, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "不能为空。");
        }
        String cleanValue = value.trim();
        if (cleanValue.length() < minLength || cleanValue.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "长度需要在 " + minLength + "-" + maxLength + " 个字符之间。");
        }
        return cleanValue;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "java";
        }
        String cleanLanguage = language.trim().toLowerCase(Locale.ROOT);
        if (!cleanLanguage.matches("[a-z0-9+#._-]{1,32}")) {
            throw new IllegalArgumentException("题解语言格式不正确。");
        }
        return cleanLanguage;
    }

    private record NormalizedSolutionInput(
            String title,
            String content,
            String language,
            String code,
            Long relatedSubmissionId
    ) {
    }
}
