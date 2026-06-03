package com.example.javacoder.model;

import java.time.Instant;

public record CurrentUser(
        long id,
        String username,
        Instant createdAt
) {
}
