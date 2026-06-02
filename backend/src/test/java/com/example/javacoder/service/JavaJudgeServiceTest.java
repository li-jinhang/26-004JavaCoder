package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.model.TestCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class JavaJudgeServiceTest {

    private final JavaJudgeService judgeService = new JavaJudgeService();

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
}
