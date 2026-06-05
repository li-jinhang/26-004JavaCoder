package com.example.javacoder.model;

import java.time.Instant;

public record AdminSolutionView(
        long id,
        long problemId,
        String problemTitle,
        String title,
        String language,
        Instant updatedAt
) {
    public static AdminSolutionView from(Solution solution) {
        return new AdminSolutionView(
                solution.id(),
                solution.problemId(),
                solution.problemTitle(),
                solution.title(),
                solution.language(),
                solution.updatedAt()
        );
    }
}
