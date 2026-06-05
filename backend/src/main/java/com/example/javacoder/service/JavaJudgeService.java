package com.example.javacoder.service;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.model.TestCase;
import com.example.javacoder.model.TestCaseResult;
import com.example.javacoder.service.sandbox.JudgeSandboxProperties;
import com.example.javacoder.service.sandbox.LanguageSpec;
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
    private final LanguageRegistry languageRegistry;

    public JavaJudgeService(
            SandboxRunner sandboxRunner,
            JudgeSandboxProperties properties,
            LanguageRegistry languageRegistry
    ) {
        this.sandboxRunner = sandboxRunner;
        this.properties = properties;
        this.languageRegistry = languageRegistry;
    }

    public Submission judge(Problem problem, SubmissionRequest request) {
        return judge(0, problem, request);
    }

    public Submission judge(long submissionId, Problem problem, SubmissionRequest request) {
        return judge(submissionId, problem, request, null);
    }

    public Submission judge(long submissionId, Problem problem, SubmissionRequest request, CurrentUser submitter) {
        Instant startedAt = Instant.now();
        LanguageSpec language = languageRegistry.findById(request.language()).orElse(null);
        if (language == null) {
            return rejected(
                    submissionId,
                    submitter,
                    problem,
                    request,
                    startedAt,
                    request.language() == null ? "unknown" : request.language(),
                    "Unsupported Language",
                    "This language is not supported yet."
            );
        }
        if (request.code() == null || request.code().isBlank()) {
            return rejected(submissionId, submitter, problem, request, startedAt, language.displayName(), "Compile Error", "Source code cannot be empty.");
        }
        if (request.code().getBytes(StandardCharsets.UTF_8).length > properties.getMaxSourceBytes()) {
            return rejected(submissionId, submitter, problem, request, startedAt, language.displayName(), "Source Limit Exceeded", "Source code exceeds the configured limit.");
        }

        try (SandboxSession session = sandboxRunner.createSession(language, request.code(), properties.limits())) {
            SandboxProcessResult compileResult = session.compile();
            if (compileResult.timedOut()) {
                return rejected(submissionId, submitter, problem, request, startedAt, language.displayName(), "Compile Timeout", "Compilation timed out.");
            }
            if (compileResult.outputLimitExceeded()) {
                return rejected(submissionId, submitter, problem, request, startedAt, language.displayName(), "Output Limit Exceeded", "Compilation output exceeds the configured limit.");
            }
            if (compileResult.exitCode() != 0) {
                return rejected(submissionId, submitter, problem, request, startedAt, language.displayName(), "Compile Error", firstUsefulLine(compileResult.stderr()));
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
                    ? "All test cases passed."
                    : caseResults.get(caseResults.size() - 1).message();

            return new Submission(
                    submissionId,
                    submitter == null ? null : submitter.id(),
                    submitter == null ? null : submitter.username(),
                    problem.id(),
                    problem.title(),
                    language.displayName(),
                    status,
                    passedCases,
                    problem.testCases().size(),
                    runtimeMs,
                    message,
                    startedAt,
                    caseResults
            );
        } catch (IOException exception) {
            return rejected(submissionId, submitter, problem, request, startedAt, language.displayName(), "Judge Error", "Sandbox error: " + exception.getMessage());
        }
    }

    private Submission rejected(
            long submissionId,
            CurrentUser submitter,
            Problem problem,
            SubmissionRequest request,
            Instant submittedAt,
            String languageName,
            String status,
            String message
    ) {
        return new Submission(
                submissionId,
                submitter == null ? null : submitter.id(),
                submitter == null ? null : submitter.username(),
                problem.id(),
                problem.title(),
                languageName,
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
            message = "Program timed out.";
        } else if (runResult.outputLimitExceeded()) {
            status = "Output Limit Exceeded";
            message = "Program output exceeds the configured limit.";
        } else if (runResult.memoryLimitExceeded()) {
            status = "Memory Limit Exceeded";
            message = "Program memory usage exceeds the configured limit.";
        } else if (runResult.exitCode() != 0) {
            status = "Runtime Error";
            message = firstUsefulLine(runResult.stderr());
        } else if (expectedOutput.equals(actualOutput)) {
            status = "Accepted";
            message = "This test case passed.";
        } else {
            status = "Wrong Answer";
            message = "Actual output does not match the expected answer.";
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
            return "No error message returned.";
        }
        return Stream.of(text.replace("\r\n", "\n").split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("No error message returned.");
    }
}
