package com.example.javacoder.controller;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.service.AdminAccountStore;
import com.example.javacoder.service.UserStore;
import com.example.javacoder.service.ai.AiCodeReviewException;
import com.example.javacoder.service.ai.AiCodeReviewService;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/submissions")
public class AiCodeReviewController {

    private final AiCodeReviewService reviewService;
    private final UserStore userStore;
    private final AdminAccountStore adminAccountStore;

    public AiCodeReviewController(
            AiCodeReviewService reviewService,
            UserStore userStore,
            AdminAccountStore adminAccountStore
    ) {
        this.reviewService = reviewService;
        this.userStore = userStore;
        this.adminAccountStore = adminAccountStore;
    }

    @GetMapping("/{submissionId}/review")
    public ResponseEntity<?> getReview(
            @PathVariable long submissionId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Optional<CurrentUser> user = requirePrincipal(authorization);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in first."));
        }

        try {
            return ResponseEntity.ok(reviewService.getExistingReview(submissionId, user.orElseThrow()));
        } catch (AiCodeReviewException exception) {
            return ResponseEntity.status(exception.status()).body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/{submissionId}/review")
    public ResponseEntity<?> createReview(
            @PathVariable long submissionId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Optional<CurrentUser> user = requirePrincipal(authorization);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in first."));
        }

        try {
            return ResponseEntity.ok(reviewService.reviewSubmission(submissionId, user.orElseThrow()));
        } catch (AiCodeReviewException exception) {
            return ResponseEntity.status(exception.status()).body(Map.of("message", exception.getMessage()));
        }
    }

    private Optional<CurrentUser> requirePrincipal(String authorization) {
        String token = extractBearerToken(authorization);
        return adminAccountStore.findByToken(token).or(() -> userStore.findByToken(token));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
