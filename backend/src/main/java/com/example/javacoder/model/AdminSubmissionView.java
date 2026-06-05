package com.example.javacoder.model;

import java.time.Instant;

public record AdminSubmissionView(
        long id,
        long problemId,
        String problemTitle,
        String language,
        String status,
        int passedCases,
        int totalCases,
        long runtimeMs,
        Instant submittedAt
) {
    public static AdminSubmissionView from(Submission submission) {
        return new AdminSubmissionView(
                submission.id(),
                submission.problemId(),
                submission.problemTitle(),
                submission.language(),
                submission.status(),
                submission.passedCases(),
                submission.totalCases(),
                submission.runtimeMs(),
                submission.submittedAt()
        );
    }
}
