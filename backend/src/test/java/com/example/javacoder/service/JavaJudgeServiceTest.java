package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.model.TestCase;
import com.example.javacoder.service.sandbox.JudgeSandboxProperties;
import com.example.javacoder.service.sandbox.LocalJavaSandboxRunner;
import java.util.List;
import org.junit.jupiter.api.Test;

class JavaJudgeServiceTest {

    private final JudgeSandboxProperties properties = testProperties();
    private final JavaJudgeService judgeService = new JavaJudgeService(new LocalJavaSandboxRunner(), properties);

    @Test
    void judgesUtf8SourceWithChineseCharacters() {
        Problem problem = new Problem(
                1L,
                "UTF-8 source",
                "Easy",
                List.of("encoding"),
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(new TestCase("", "你好，世界", false))
        );
        SubmissionRequest request = new SubmissionRequest(
                1L,
                "java",
                """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("你好，世界");
                    }
                }
                """
        );

        Submission submission = judgeService.judge(problem, request);

        assertThat(submission.status()).isEqualTo("Accepted");
        assertThat(submission.passedCases()).isEqualTo(1);
    }

    @Test
    void decodesCompilerDiagnosticsAsUtf8() {
        Problem problem = new Problem(
                2L,
                "Compiler diagnostics",
                "Easy",
                List.of("encoding"),
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(new TestCase("", "", false))
        );
        SubmissionRequest request = new SubmissionRequest(
                2L,
                "java",
                """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(missingSymbol);
                    }
                }
                """
        );

        Submission submission = judgeService.judge(problem, request);

        assertThat(submission.status()).isEqualTo("Compile Error");
        assertThat(submission.message()).doesNotContain("\uFFFD");
    }

    @Test
    void rejectsSourceThatExceedsConfiguredLimit() {
        Problem problem = simpleProblem();
        properties.setMaxSourceBytes(32);

        Submission submission = judgeService.judge(problem, new SubmissionRequest(
                1L,
                "java",
                "public class Main { public static void main(String[] args) { System.out.println(1); } }"
        ));

        assertThat(submission.status()).isEqualTo("Source Limit Exceeded");
    }

    @Test
    void stopsProgramsThatRunTooLong() {
        Problem problem = simpleProblem();

        Submission submission = judgeService.judge(problem, new SubmissionRequest(
                1L,
                "java",
                """
                public class Main {
                    public static void main(String[] args) {
                        while (true) {
                        }
                    }
                }
                """
        ));

        assertThat(submission.status()).isEqualTo("Time Limit Exceeded");
    }

    @Test
    void stopsProgramsThatProduceTooMuchOutput() {
        Problem problem = simpleProblem();
        properties.setMaxOutputBytes(256);

        Submission submission = judgeService.judge(problem, new SubmissionRequest(
                1L,
                "java",
                """
                public class Main {
                    public static void main(String[] args) {
                        for (int i = 0; i < 10000; i++) {
                            System.out.println("too much output");
                        }
                    }
                }
                """
        ));

        assertThat(submission.status()).isEqualTo("Output Limit Exceeded");
    }

    @Test
    void reportsMemoryLimitWhenProgramRunsOutOfHeap() {
        Problem problem = simpleProblem();

        Submission submission = judgeService.judge(problem, new SubmissionRequest(
                1L,
                "java",
                """
                public class Main {
                    public static void main(String[] args) {
                        throw new OutOfMemoryError("simulated heap exhaustion");
                    }
                }
                """
        ));

        assertThat(submission.status()).isEqualTo("Memory Limit Exceeded");
    }

    private Problem simpleProblem() {
        return new Problem(
                1L,
                "Simple",
                "Easy",
                List.of("sandbox"),
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(new TestCase("", "ok", false))
        );
    }

    private JudgeSandboxProperties testProperties() {
        JudgeSandboxProperties sandboxProperties = new JudgeSandboxProperties();
        sandboxProperties.setMode("local");
        sandboxProperties.setRunTimeout(java.time.Duration.ofMillis(500));
        sandboxProperties.setCompileTimeout(java.time.Duration.ofSeconds(8));
        sandboxProperties.setMaxSourceBytes(65536);
        sandboxProperties.setMaxOutputBytes(65536);
        return sandboxProperties;
    }
}
