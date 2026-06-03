package com.example.javacoder.service.sandbox;

public record SandboxProcessResult(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut,
        boolean outputLimitExceeded,
        boolean memoryLimitExceeded,
        long durationMs
) {
}
