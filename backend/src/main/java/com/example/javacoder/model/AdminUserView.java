package com.example.javacoder.model;

import java.time.Instant;

public record AdminUserView(
        long id,
        String username,
        UserRole role,
        Instant createdAt,
        boolean currentUser
) {
    public static AdminUserView from(CurrentUser user, long currentUserId) {
        return new AdminUserView(
                user.id(),
                user.username(),
                user.role(),
                user.createdAt(),
                user.id() == currentUserId
        );
    }
}
