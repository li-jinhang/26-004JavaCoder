package com.example.javacoder.model;

import java.util.List;

public record ProblemDetail(
        long id,
        String title,
        String difficulty,
        List<String> tags,
        String description,
        String inputFormat,
        String outputFormat,
        String constraints,
        String starterCode,
        List<ExampleCase> examples,
        int visibleCaseCount,
        int totalCaseCount
) {
    public static ProblemDetail from(Problem problem) {
        return new ProblemDetail(
                problem.id(),
                problem.title(),
                problem.difficulty(),
                problem.tags(),
                problem.description(),
                problem.inputFormat(),
                problem.outputFormat(),
                problem.constraints(),
                problem.starterCode(),
                problem.examples(),
                problem.visibleCaseCount(),
                problem.totalCaseCount()
        );
    }
}
