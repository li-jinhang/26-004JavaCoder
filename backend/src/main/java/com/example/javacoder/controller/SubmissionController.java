package com.example.javacoder.controller;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.repository.ProblemRepository;
import com.example.javacoder.service.AdminAccountStore;
import com.example.javacoder.service.JudgeWorker;
import com.example.javacoder.service.SubmissionStore;
import com.example.javacoder.service.UserStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final ProblemRepository problemRepository;
    private final JudgeWorker judgeWorker;
    private final SubmissionStore submissionStore;
    private final UserStore userStore;
    private final AdminAccountStore adminAccountStore;

    public SubmissionController(
            ProblemRepository problemRepository,
            JudgeWorker judgeWorker,
            SubmissionStore submissionStore,
            UserStore userStore,
            AdminAccountStore adminAccountStore
    ) {
        this.problemRepository = problemRepository;
        this.judgeWorker = judgeWorker;
        this.submissionStore = submissionStore;
        this.userStore = userStore;
        this.adminAccountStore = adminAccountStore;
    }

    @GetMapping
    public List<Submission> listSubmissions() {
        return submissionStore.findRecent();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Submission> getSubmission(@PathVariable long id) {
        return submissionStore.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> submit(
            @RequestBody SubmissionRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        String token = extractBearerToken(authorization);
        Optional<CurrentUser> submitter = userStore.findByToken(token).or(() -> adminAccountStore.findByToken(token));
        if (submitter.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "请先登录后再提交代码。"));
        }

        return problemRepository.findById(request.problemId())
                .<ResponseEntity<?>>map(problem -> {
                    Submission submission = judgeWorker.enqueue(problem, request, submitter.orElseThrow());
                    return ResponseEntity.accepted().body(submission);
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
