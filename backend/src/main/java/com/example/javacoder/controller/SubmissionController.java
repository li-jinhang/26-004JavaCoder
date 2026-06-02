package com.example.javacoder.controller;

import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.repository.ProblemRepository;
import com.example.javacoder.service.JavaJudgeService;
import com.example.javacoder.service.SubmissionStore;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final ProblemRepository problemRepository;
    private final JavaJudgeService javaJudgeService;
    private final SubmissionStore submissionStore;

    public SubmissionController(
            ProblemRepository problemRepository,
            JavaJudgeService javaJudgeService,
            SubmissionStore submissionStore
    ) {
        this.problemRepository = problemRepository;
        this.javaJudgeService = javaJudgeService;
        this.submissionStore = submissionStore;
    }

    @GetMapping
    public List<Submission> listSubmissions() {
        return submissionStore.findRecent();
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody SubmissionRequest request) {
        return problemRepository.findById(request.problemId())
                .<ResponseEntity<?>>map(problem -> {
                    Submission submission = javaJudgeService.judge(problem, request);
                    submissionStore.save(submission);
                    return ResponseEntity.ok(submission);
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("message", "题目不存在。")));
    }
}
