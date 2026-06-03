package com.example.javacoder.model;

import java.util.List;

public record Problem(
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
        List<TestCase> testCases,
        String referenceSolution
) {
    public Problem(
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
            List<TestCase> testCases
    ) {
        this(
                id,
                title,
                difficulty,
                tags,
                description,
                inputFormat,
                outputFormat,
                constraints,
                starterCode,
                examples,
                testCases,
                ""
        );
    }

    public int visibleCaseCount() {
        return (int) testCases.stream().filter(testCase -> !testCase.hidden()).count();
    }

    public int totalCaseCount() {
        return testCases.size();
    }
}
