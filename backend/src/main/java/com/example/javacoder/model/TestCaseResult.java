package com.example.javacoder.model;

public record TestCaseResult(
        int caseNumber,
        boolean hidden,
        String status,
        String input,
        String expectedOutput,
        String actualOutput,
        long runtimeMs,
        String message
) {
}
