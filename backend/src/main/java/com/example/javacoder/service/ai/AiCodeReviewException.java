package com.example.javacoder.service.ai;

import org.springframework.http.HttpStatus;

public class AiCodeReviewException extends RuntimeException {

    private final HttpStatus status;

    public AiCodeReviewException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
