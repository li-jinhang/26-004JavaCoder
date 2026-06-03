package com.example.javacoder.model;

public record AuthResponse(
        String token,
        CurrentUser user
) {
}
