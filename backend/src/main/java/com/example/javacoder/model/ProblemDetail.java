package com.example.javacoder.model;

import java.util.List;
import java.util.Map;

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
        Map<String, String> starterCodes,
        List<ExampleCase> examples,
        String referenceSolution,
        Map<String, String> referenceSolutions,
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
                problem.starterCodes(),
                problem.examples(),
                problem.referenceSolution(),
                problem.referenceSolutions(),
                problem.visibleCaseCount(),
                problem.totalCaseCount()
        );
    }
}
