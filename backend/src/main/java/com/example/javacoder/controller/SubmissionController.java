package com.example.javacoder.controller;

import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.repository.ProblemRepository;
import com.example.javacoder.service.JavaJudgeService;
import com.example.javacoder.service.SubmissionStore;
import com.example.javacoder.service.UserStore;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final ProblemRepository problemRepository;
    private final JavaJudgeService javaJudgeService;
    private final SubmissionStore submissionStore;
    private final UserStore userStore;

    public SubmissionController(
            ProblemRepository problemRepository,
            JavaJudgeService javaJudgeService,
            SubmissionStore submissionStore,
            UserStore userStore
    ) {
        this.problemRepository = problemRepository;
        this.javaJudgeService = javaJudgeService;
        this.submissionStore = submissionStore;
        this.userStore = userStore;
    }

    @GetMapping
    public List<Submission> listSubmissions() {
        return submissionStore.findRecent();
    }

    @PostMapping
    public ResponseEntity<?> submit(
            @RequestBody SubmissionRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        if (userStore.findByToken(extractBearerToken(authorization)).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "请先登录后再提交代码。"));
        }

        return problemRepository.findById(request.problemId())
                .<ResponseEntity<?>>map(problem -> {
                    Submission submission = javaJudgeService.judge(problem, request);
                    submissionStore.save(submission);
                    return ResponseEntity.ok(submission);
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("message", "题目不存在。")));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
