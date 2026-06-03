package com.example.javacoder.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProblemRepositoryTest {

    @Test
    void exposesTwentySequentialProblems() {
        ProblemRepository repository = new ProblemRepository();

        assertThat(repository.findAll())
                .hasSize(20)
                .extracting(problem -> problem.id())
                .containsExactlyInAnyOrderElementsOf(
                        java.util.stream.LongStream.rangeClosed(1, 20)
                                .boxed()
                                .toList()
                );
    }

    @Test
    void loadsReferenceSolutionsFromResources() {
        ProblemRepository repository = new ProblemRepository();

        assertThat(repository.findAll())
                .allSatisfy(problem -> assertThat(problem.referenceSolution())
                        .contains("public class Main"));
    }
}
