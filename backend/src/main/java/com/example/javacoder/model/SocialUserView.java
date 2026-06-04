package com.example.javacoder.model;

import java.time.Instant;

public record SocialUserView(
        long id,
        String username,
        Instant createdAt,
        boolean friend,
        boolean currentUser
) {
    public static SocialUserView from(CurrentUser user, boolean friend, long currentUserId) {
        return new SocialUserView(
                user.id(),
                user.username(),
                user.createdAt(),
                friend,
                user.id() == currentUserId
        );
    }
}
