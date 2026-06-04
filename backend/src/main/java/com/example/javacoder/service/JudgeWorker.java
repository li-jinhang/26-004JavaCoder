package com.example.javacoder.service;

import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class JudgeWorker implements AutoCloseable {

    private final JavaJudgeService javaJudgeService;
    private final SubmissionStore submissionStore;
    private final ExecutorService executorService;

    public JudgeWorker(JavaJudgeService javaJudgeService, SubmissionStore submissionStore) {
        this.javaJudgeService = javaJudgeService;
        this.submissionStore = submissionStore;
        this.executorService = Executors.newFixedThreadPool(2, new JudgeThreadFactory());
    }

    public Submission enqueue(Problem problem, SubmissionRequest request) {
        Submission pending = new Submission(
                submissionStore.nextId(),
                problem.id(),
                problem.title(),
                request.language() == null ? "Java" : request.language(),
                "Pending",
                0,
                problem.testCases().size(),
                0,
                "提交已进入评测队列。",
                Instant.now(),
                List.of()
        );
        submissionStore.save(pending);

        try {
            executorService.submit(() -> judge(pending.id(), problem, request));
        } catch (RejectedExecutionException exception) {
            submissionStore.update(withStatus(pending, "Judge Error", "评测队列已关闭。"));
        }

        return pending;
    }

    private void judge(long id, Problem problem, SubmissionRequest request) {
        submissionStore.findById(id)
                .map(submission -> withStatus(submission, "Judging", "正在沙箱中编译并运行。"))
                .ifPresent(submissionStore::update);

        Submission judged = javaJudgeService.judge(id, problem, request);
        submissionStore.findById(id)
                .map(current -> withSubmittedAt(judged, current.submittedAt()))
                .ifPresentOrElse(submissionStore::update, () -> submissionStore.update(judged));
    }

    private Submission withStatus(Submission submission, String status, String message) {
        return new Submission(
                submission.id(),
                submission.problemId(),
                submission.problemTitle(),
                submission.language(),
                status,
                submission.passedCases(),
                submission.totalCases(),
                submission.runtimeMs(),
                message,
                submission.submittedAt(),
                submission.caseResults()
        );
    }

    private Submission withSubmittedAt(Submission submission, Instant submittedAt) {
        return new Submission(
                submission.id(),
                submission.problemId(),
                submission.problemTitle(),
                submission.language(),
                submission.status(),
                submission.passedCases(),
                submission.totalCases(),
                submission.runtimeMs(),
                submission.message(),
                submittedAt,
                submission.caseResults()
        );
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }

    private static class JudgeThreadFactory implements ThreadFactory {

        private final AtomicInteger threadId = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "javacoder-judge-" + threadId.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
