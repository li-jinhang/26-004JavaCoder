package com.example.javacoder.service;

import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.model.TestCase;
import com.example.javacoder.model.TestCaseResult;
import com.example.javacoder.service.sandbox.JudgeSandboxProperties;
import com.example.javacoder.service.sandbox.SandboxProcessResult;
import com.example.javacoder.service.sandbox.SandboxRunner;
import com.example.javacoder.service.sandbox.SandboxSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(JudgeSandboxProperties.class)
public class JavaJudgeService {

    private final SandboxRunner sandboxRunner;
    private final JudgeSandboxProperties properties;

    public JavaJudgeService(SandboxRunner sandboxRunner, JudgeSandboxProperties properties) {
        this.sandboxRunner = sandboxRunner;
        this.properties = properties;
    }

    public Submission judge(Problem problem, SubmissionRequest request) {
        return judge(0, problem, request);
    }

    public Submission judge(long submissionId, Problem problem, SubmissionRequest request) {
        Instant startedAt = Instant.now();
        if (!"java".equalsIgnoreCase(request.language())) {
            return rejected(submissionId, problem, request, startedAt, "Unsupported Language", "当前评测器仅支持 Java。");
        }
        if (request.code() == null || request.code().isBlank()) {
            return rejected(submissionId, problem, request, startedAt, "Compile Error", "代码不能为空。");
        }
        if (request.code().getBytes(StandardCharsets.UTF_8).length > properties.getMaxSourceBytes()) {
            return rejected(submissionId, problem, request, startedAt, "Source Limit Exceeded", "源码大小超过限制。");
        }

        try (SandboxSession session = sandboxRunner.createSession(request.code(), properties.limits())) {
            SandboxProcessResult compileResult = session.compile();
            if (compileResult.timedOut()) {
                return rejected(submissionId, problem, request, startedAt, "Compile Timeout", "编译超时。");
            }
            if (compileResult.outputLimitExceeded()) {
                return rejected(submissionId, problem, request, startedAt, "Output Limit Exceeded", "编译输出超过限制。");
            }
            if (compileResult.exitCode() != 0) {
                return rejected(submissionId, problem, request, startedAt, "Compile Error", firstUsefulLine(compileResult.stderr()));
            }

            List<TestCaseResult> caseResults = new ArrayList<>();
            int passedCases = 0;
            long runtimeMs = 0;
            for (int index = 0; index < problem.testCases().size(); index++) {
                TestCase testCase = problem.testCases().get(index);
                SandboxProcessResult runResult = session.run(testCase.input());
                runtimeMs += runResult.durationMs();

                TestCaseResult caseResult = toCaseResult(index + 1, testCase, runResult);
                caseResults.add(caseResult);
                if ("Accepted".equals(caseResult.status())) {
                    passedCases++;
                } else {
                    break;
                }
            }

            String status = passedCases == problem.testCases().size()
                    ? "Accepted"
                    : caseResults.get(caseResults.size() - 1).status();
            String message = "Accepted".equals(status)
                    ? "所有测试用例均已通过。"
                    : caseResults.get(caseResults.size() - 1).message();

            return new Submission(
                    submissionId,
                    problem.id(),
                    problem.title(),
                    "Java",
                    status,
                    passedCases,
                    problem.testCases().size(),
                    runtimeMs,
                    message,
                    startedAt,
                    caseResults
            );
        } catch (IOException exception) {
            return rejected(submissionId, problem, request, startedAt, "Judge Error", "评测沙箱错误：" + exception.getMessage());
        }
    }

    private Submission rejected(
            long submissionId,
            Problem problem,
            SubmissionRequest request,
            Instant submittedAt,
            String status,
            String message
    ) {
        return new Submission(
                submissionId,
                problem.id(),
                problem.title(),
                request.language() == null ? "Java" : request.language(),
                status,
                0,
                problem.testCases().size(),
                0,
                message,
                submittedAt,
                List.of()
        );
    }

    private TestCaseResult toCaseResult(int caseNumber, TestCase testCase, SandboxProcessResult runResult) {
        String actualOutput = normalize(runResult.stdout());
        String expectedOutput = normalize(testCase.expectedOutput());
        String status;
        String message;

        if (runResult.timedOut()) {
            status = "Time Limit Exceeded";
            message = "程序运行超时。";
        } else if (runResult.outputLimitExceeded()) {
            status = "Output Limit Exceeded";
            message = "程序输出超过限制。";
        } else if (runResult.memoryLimitExceeded()) {
            status = "Memory Limit Exceeded";
            message = "程序内存使用超过限制。";
        } else if (runResult.exitCode() != 0) {
            status = "Runtime Error";
            message = firstUsefulLine(runResult.stderr());
        } else if (expectedOutput.equals(actualOutput)) {
            status = "Accepted";
            message = "该用例已通过。";
        } else {
            status = "Wrong Answer";
            message = "实际输出与期望答案不一致。";
        }

        return new TestCaseResult(
                caseNumber,
                testCase.hidden(),
                status,
                testCase.hidden() ? null : testCase.input(),
                testCase.hidden() ? null : expectedOutput,
                testCase.hidden() ? null : actualOutput,
                runResult.durationMs(),
                message
        );
    }

    private String normalize(String output) {
        if (output == null) {
            return "";
        }
        String normalized = output.replace("\r\n", "\n").replace('\r', '\n').trim();
        return normalized.isEmpty() ? "" : normalized.replaceAll("\\s+", " ");
    }

    private String firstUsefulLine(String text) {
        if (text == null || text.isBlank()) {
            return "未返回错误信息。";
        }
        return Stream.of(text.replace("\r\n", "\n").split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("未返回错误信息。");
    }
}
