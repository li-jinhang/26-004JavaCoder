package com.example.javacoder.model;

import java.time.Instant;

public record Solution(
        long id,
        long problemId,
        String problemTitle,
        long authorId,
        String authorName,
        String title,
        String content,
        String language,
        String code,
        Long relatedSubmissionId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
