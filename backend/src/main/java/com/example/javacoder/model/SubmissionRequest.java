package com.example.javacoder.model;

public record SubmissionRequest(
        long problemId,
        String language,
        String code
) {
}
