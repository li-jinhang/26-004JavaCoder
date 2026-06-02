package com.example.javacoder.model;

import java.time.Instant;
import java.util.List;

public record Submission(
        long id,
        long problemId,
        String problemTitle,
        String language,
        String status,
        int passedCases,
        int totalCases,
        long runtimeMs,
        String message,
        Instant submittedAt,
        List<TestCaseResult> caseResults
) {
}
