package com.example.javacoder.model;

import java.util.List;
import java.util.Map;

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
        Map<String, String> starterCodes,
        List<ExampleCase> examples,
        List<TestCase> testCases,
        String referenceSolution,
        Map<String, String> referenceSolutions
) {
    public Problem {
        starterCodes = starterCodes == null ? Map.of() : Map.copyOf(starterCodes);
        referenceSolutions = referenceSolutions == null ? Map.of() : Map.copyOf(referenceSolutions);
    }

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
                Map.of(),
                examples,
                testCases,
                "",
                Map.of()
        );
    }

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
            List<TestCase> testCases,
            String referenceSolution
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
                Map.of(),
                examples,
                testCases,
                referenceSolution,
                Map.of()
        );
    }

    public int visibleCaseCount() {
        return (int) testCases.stream().filter(testCase -> !testCase.hidden()).count();
    }

    public int totalCaseCount() {
        return testCases.size();
    }
}
