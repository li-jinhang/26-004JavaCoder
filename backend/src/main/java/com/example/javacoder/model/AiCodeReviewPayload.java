package com.example.javacoder.model;

import java.util.List;

public record AiCodeReviewPayload(
        String summary,
        Integer score,
        String correctness,
        String complexity,
        List<String> bugs,
        List<String> improvements,
        List<String> nextSteps
) {
}
