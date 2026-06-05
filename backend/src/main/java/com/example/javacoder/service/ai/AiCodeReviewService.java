package com.example.javacoder.service.ai;

import com.example.javacoder.model.AiCodeReview;
import com.example.javacoder.model.AiCodeReviewPayload;
import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionSource;
import com.example.javacoder.model.UserRole;
import com.example.javacoder.repository.ProblemRepository;
import com.example.javacoder.service.SubmissionStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiCodeReviewService {

    private final SubmissionStore submissionStore;
    private final ProblemRepository problemRepository;
    private final AiCodeReviewStore reviewStore;
    private final AiCodeReviewPromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AiCodeReviewService(
            SubmissionStore submissionStore,
            ProblemRepository problemRepository,
            AiCodeReviewStore reviewStore,
            AiCodeReviewPromptBuilder promptBuilder,
            LlmClient llmClient,
            ObjectMapper objectMapper
    ) {
        this.submissionStore = submissionStore;
        this.problemRepository = problemRepository;
        this.reviewStore = reviewStore;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public AiCodeReview getExistingReview(long submissionId, CurrentUser user) {
        Submission submission = requireSubmission(submissionId);
        requireAccess(submission, user);
        return reviewStore.findBySubmissionId(submissionId)
                .orElseThrow(() -> new AiCodeReviewException(HttpStatus.NOT_FOUND, "AI review does not exist yet."));
    }

    public AiCodeReview reviewSubmission(long submissionId, CurrentUser user) {
        Submission submission = requireSubmission(submissionId);
        requireAccess(submission, user);
        ensureJudged(submission);

        return reviewStore.findBySubmissionId(submissionId)
                .orElseGet(() -> createReview(submission));
    }

    private AiCodeReview createReview(Submission submission) {
        Problem problem = problemRepository.findById(submission.problemId())
                .orElseThrow(() -> new AiCodeReviewException(HttpStatus.NOT_FOUND, "Problem does not exist."));
        SubmissionSource source = submissionStore.findSourceById(submission.id())
                .orElseThrow(() -> new AiCodeReviewException(HttpStatus.CONFLICT, "Submission source code was not recorded."));

        String response;
        try {
            response = llmClient.completeJson(
                    promptBuilder.systemPrompt(),
                    promptBuilder.userPrompt(problem, submission, source)
            );
        } catch (LlmException exception) {
            throw new AiCodeReviewException(exception.status(), exception.getMessage());
        }

        AiCodeReviewPayload payload = parsePayload(response);
        return reviewStore.create(new AiCodeReview(
                0,
                submission.id(),
                submission.userId(),
                submission.username(),
                submission.problemId(),
                submission.problemTitle(),
                normalizeText(payload.summary(), "No summary returned."),
                clampScore(payload.score()),
                normalizeText(payload.correctness(), "No correctness analysis returned."),
                normalizeText(payload.complexity(), "No complexity analysis returned."),
                normalizeList(payload.bugs()),
                normalizeList(payload.improvements()),
                normalizeList(payload.nextSteps()),
                Instant.now()
        ));
    }

    private Submission requireSubmission(long submissionId) {
        return submissionStore.findById(submissionId)
                .orElseThrow(() -> new AiCodeReviewException(HttpStatus.NOT_FOUND, "Submission does not exist."));
    }

    private void requireAccess(Submission submission, CurrentUser user) {
        if (user.role() == UserRole.ADMIN) {
            return;
        }
        if (submission.userId() == null || !submission.userId().equals(user.id())) {
            throw new AiCodeReviewException(HttpStatus.FORBIDDEN, "You can only review your own submissions.");
        }
    }

    private void ensureJudged(Submission submission) {
        if ("Pending".equals(submission.status()) || "Judging".equals(submission.status())) {
            throw new AiCodeReviewException(HttpStatus.CONFLICT, "Submission is still being judged.");
        }
    }

    private AiCodeReviewPayload parsePayload(String response) {
        try {
            return objectMapper.readValue(extractJsonObject(response), AiCodeReviewPayload.class);
        } catch (JsonProcessingException exception) {
            throw new AiCodeReviewException(HttpStatus.BAD_GATEWAY, "LLM returned invalid review JSON.");
        }
    }

    private String extractJsonObject(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }

        String text = response.trim();
        int start = text.indexOf('{');
        if (start < 0) {
            return text;
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = inString;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, index + 1);
                }
            }
        }

        return text.substring(start);
    }

    private String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private int clampScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .limit(10)
                .toList();
    }
}
