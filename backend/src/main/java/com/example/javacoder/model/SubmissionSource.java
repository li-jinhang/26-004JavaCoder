package com.example.javacoder.model;

public record SubmissionSource(
        long submissionId,
        String language,
        String code
) {
}
