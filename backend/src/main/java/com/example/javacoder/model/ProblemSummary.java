package com.example.javacoder.model;

import java.util.List;

public record ProblemSummary(
        long id,
        String title,
        String difficulty,
        List<String> tags,
        int acceptedCount,
        int submissionCount
) {
}
