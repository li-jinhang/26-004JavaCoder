package com.example.javacoder.model;

import java.time.Instant;

public record FriendView(
        long id,
        String username,
        Instant createdAt,
        Instant friendSince,
        ChatMessage lastMessage,
        int unreadCount
) {
}
