package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.model.TestCase;
import com.example.javacoder.service.sandbox.JudgeSandboxProperties;
import com.example.javacoder.service.sandbox.JudgeLimits;
import com.example.javacoder.service.sandbox.SandboxProcessResult;
import com.example.javacoder.service.sandbox.SandboxRunner;
import com.example.javacoder.service.sandbox.SandboxSession;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JudgeWorkerTest {

    @Test
    void returnsPendingSubmissionAndUpdatesItAfterBackgroundJudging() {
        SubmissionStore submissionStore = new SubmissionStore();
        JavaJudgeService judgeService = new JavaJudgeService(new AcceptingSandboxRunner(), new JudgeSandboxProperties());
        JudgeWorker judgeWorker = new JudgeWorker(judgeService, submissionStore);
        Problem problem = new Problem(
                1L,
                "Async",
                "Easy",
                List.of("queue"),
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(new TestCase("", "ok", false))
        );

        Submission pending = judgeWorker.enqueue(problem, new SubmissionRequest(
                1L,
                "java",
                "public class Main { public static void main(String[] args) {} }"
        ));

        assertThat(pending.status()).isEqualTo("Pending");
        assertThat(submissionStore.findById(pending.id())).hasValueSatisfying(
                submission -> assertThat(submission.status()).isIn("Pending", "Judging", "Accepted")
        );

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            Optional<Submission> stored = submissionStore.findById(pending.id());
            assertThat(stored).isPresent();
            assertThat(stored.orElseThrow().status()).isEqualTo("Accepted");
            assertThat(stored.orElseThrow().passedCases()).isEqualTo(1);
        });

        judgeWorker.close();
    }

    private static class AcceptingSandboxRunner implements SandboxRunner {

        @Override
        public SandboxSession createSession(String sourceCode, JudgeLimits limits) {
            return new SandboxSession() {
                @Override
                public SandboxProcessResult compile() {
                    return new SandboxProcessResult(0, "", "", false, false, false, 10);
                }

                @Override
                public SandboxProcessResult run(String input) {
                    return new SandboxProcessResult(0, "ok", "", false, false, false, 20);
                }

                @Override
                public void close() {
                }
            };
        }
    }
}
