package com.example.javacoder.model;

import java.time.Instant;
import java.util.List;

public record AdminUserView(
        long id,
        String username,
        UserRole role,
        Instant createdAt,
        boolean currentUser,
        long totalSubmissions,
        long acceptedSubmissions,
        List<AdminSubmissionView> recentSubmissions,
        long totalSolutions,
        List<AdminSolutionView> recentSolutions
) {
    public static AdminUserView from(CurrentUser user, long currentUserId) {
        return from(user, currentUserId, 0, 0, List.of(), 0, List.of());
    }

    public static AdminUserView from(
            CurrentUser user,
            long currentUserId,
            long totalSubmissions,
            long acceptedSubmissions,
            List<AdminSubmissionView> recentSubmissions,
            long totalSolutions,
            List<AdminSolutionView> recentSolutions
    ) {
        return new AdminUserView(
                user.id(),
                user.username(),
                user.role(),
                user.createdAt(),
                user.id() == currentUserId,
                totalSubmissions,
                acceptedSubmissions,
                recentSubmissions,
                totalSolutions,
                recentSolutions
        );
    }
}
