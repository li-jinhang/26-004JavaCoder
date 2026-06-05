package com.example.javacoder.model;

import java.time.Instant;
import java.util.List;

public record AiCodeReview(
        long id,
        long submissionId,
        Long userId,
        String username,
        long problemId,
        String problemTitle,
        String summary,
        int score,
        String correctness,
        String complexity,
        List<String> bugs,
        List<String> improvements,
        List<String> nextSteps,
        Instant createdAt
) {
}
