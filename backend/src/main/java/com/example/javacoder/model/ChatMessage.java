package com.example.javacoder.model;

import java.time.Instant;

public record ChatMessage(
        long id,
        long senderId,
        String senderName,
        long recipientId,
        String recipientName,
        String type,
        String content,
        Long problemId,
        String problemTitle,
        String code,
        Instant createdAt,
        Instant readAt
) {
}
