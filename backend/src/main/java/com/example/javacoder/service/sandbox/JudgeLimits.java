package com.example.javacoder.service.sandbox;

import java.time.Duration;

public record JudgeLimits(
        Duration compileTimeout,
        Duration runTimeout,
        int maxSourceBytes,
        int maxOutputBytes
) {
}
