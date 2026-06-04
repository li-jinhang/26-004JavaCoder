package com.example.javacoder.model;

public record ChatMessageRequest(
        long recipientId,
        String type,
        String content,
        Long problemId,
        String problemTitle,
        String code
) {
}
