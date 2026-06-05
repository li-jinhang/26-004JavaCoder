package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.ExampleCase;
import com.example.javacoder.model.Problem;
import com.example.javacoder.model.SolutionRequest;
import com.example.javacoder.model.TestCase;
import com.example.javacoder.model.UserRole;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SolutionStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void createsUpdatesListsAndSoftDeletesSolutions() {
        SolutionStore firstStore = new SolutionStore(sqliteJdbcTemplate());
        Problem problem = simpleProblem();
        CurrentUser author = new CurrentUser(101, "Alice", UserRole.USER, Instant.parse("2026-06-04T00:00:00Z"));

        var created = firstStore.create(problem, author, new SolutionRequest(
                "Prefix sum idea",
                "Use a running prefix sum and compare every target in one pass.",
                "Java",
                "public class Main {}",
                1001L
        ));

        SolutionStore secondStore = new SolutionStore(sqliteJdbcTemplate());

        assertThat(secondStore.findById(created.id())).hasValueSatisfying(solution -> {
            assertThat(solution.problemTitle()).isEqualTo("Two Sum");
            assertThat(solution.authorName()).isEqualTo("Alice");
            assertThat(solution.language()).isEqualTo("java");
            assertThat(solution.relatedSubmissionId()).isEqualTo(1001L);
        });
        assertThat(secondStore.findByProblemId(problem.id())).hasSize(1);
        assertThat(secondStore.countByAuthorId(author.id())).isEqualTo(1);
        assertThat(secondStore.findRecentByAuthorId(author.id(), 5)).containsExactly(created);

        var updated = secondStore.update(created.id(), new SolutionRequest(
                "Hash table idea",
                "Use a hash table to remember the values that have already appeared.",
                "java",
                "",
                null
        ));

        assertThat(updated.title()).isEqualTo("Hash table idea");
        assertThat(updated.relatedSubmissionId()).isNull();
        assertThat(secondStore.delete(created.id())).isTrue();
        assertThat(secondStore.findById(created.id())).isEmpty();
        assertThat(secondStore.findByProblemId(problem.id())).isEmpty();
        assertThat(secondStore.countByAuthorId(author.id())).isZero();
        assertThat(secondStore.findRecentByAuthorId(author.id(), 5)).isEmpty();
    }

    @Test
    void validatesRequiredFields() {
        SolutionStore store = new SolutionStore(sqliteJdbcTemplate());

        assertThatThrownBy(() -> store.create(simpleProblem(), new CurrentUser(101, "Alice", UserRole.USER, Instant.now()),
                new SolutionRequest("hi", "too short", "java", "", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("题解标题");
    }

    private Problem simpleProblem() {
        return new Problem(
                7,
                "Two Sum",
                "简单",
                List.of("数组"),
                "Find two numbers.",
                "numbers",
                "indexes",
                "n <= 1000",
                "public class Main {}",
                List.of(new ExampleCase("1 2", "3", "sample")),
                List.of(new TestCase("1 2", "3", false)),
                "public class Main {}"
        );
    }

    private JdbcTemplate sqliteJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("javacoder-test.sqlite"));
        return new JdbcTemplate(dataSource);
    }
}
