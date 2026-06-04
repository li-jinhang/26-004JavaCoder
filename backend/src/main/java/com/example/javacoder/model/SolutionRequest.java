package com.example.javacoder.model;

public record SolutionRequest(
        String title,
        String content,
        String language,
        String code,
        Long relatedSubmissionId
) {
}
