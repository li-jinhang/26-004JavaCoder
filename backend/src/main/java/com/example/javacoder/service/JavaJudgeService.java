package com.example.javacoder.service;

import com.example.javacoder.model.Problem;
import com.example.javacoder.model.Submission;
import com.example.javacoder.model.SubmissionRequest;
import com.example.javacoder.model.TestCase;
import com.example.javacoder.model.TestCaseResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class JavaJudgeService {

    private static final String UTF_8 = StandardCharsets.UTF_8.name();
    private static final Duration COMPILE_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration RUN_TIMEOUT = Duration.ofSeconds(3);
    private final AtomicLong submissionId = new AtomicLong(1000);

    public Submission judge(Problem problem, SubmissionRequest request) {
        Instant startedAt = Instant.now();
        if (!"java".equalsIgnoreCase(request.language())) {
            return rejected(problem, request, startedAt, "Unsupported Language", "当前演示评测器仅支持 Java。");
        }
        if (request.code() == null || request.code().isBlank()) {
            return rejected(problem, request, startedAt, "Compile Error", "代码不能为空。");
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("javacoder-judge-");
            Files.writeString(workDir.resolve("Main.java"), request.code(), StandardCharsets.UTF_8);

            ProcessResult compileResult = runProcess(
                    compileCommand(),
                    workDir,
                    null,
                    COMPILE_TIMEOUT
            );
            if (compileResult.timedOut()) {
                return rejected(problem, request, startedAt, "Compile Timeout", "编译超时。");
            }
            if (compileResult.exitCode() != 0) {
                return rejected(problem, request, startedAt, "Compile Error", firstUsefulLine(compileResult.stderr()));
            }

            List<TestCaseResult> caseResults = new ArrayList<>();
            int passedCases = 0;
            long runtimeMs = 0;
            for (int index = 0; index < problem.testCases().size(); index++) {
                TestCase testCase = problem.testCases().get(index);
                ProcessResult runResult = runProcess(
                        runCommand(workDir),
                        workDir,
                        testCase.input(),
                        RUN_TIMEOUT
                );
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
                    submissionId.incrementAndGet(),
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
            return rejected(problem, request, startedAt, "Judge Error", "评测环境错误：" + exception.getMessage());
        } finally {
            if (workDir != null) {
                deleteQuietly(workDir);
            }
        }
    }

    private List<String> compileCommand() {
        return List.of(
                "javac",
                "-encoding", UTF_8,
                "-J-Dfile.encoding=" + UTF_8,
                "-J-Dsun.stdout.encoding=" + UTF_8,
                "-J-Dsun.stderr.encoding=" + UTF_8,
                "Main.java"
        );
    }

    private List<String> runCommand(Path workDir) {
        return List.of(
                "java",
                "-Dfile.encoding=" + UTF_8,
                "-Dsun.stdout.encoding=" + UTF_8,
                "-Dsun.stderr.encoding=" + UTF_8,
                "-cp", workDir.toString(),
                "Main"
        );
    }

    private Submission rejected(
            Problem problem,
            SubmissionRequest request,
            Instant submittedAt,
            String status,
            String message
    ) {
        return new Submission(
                submissionId.incrementAndGet(),
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

    private TestCaseResult toCaseResult(int caseNumber, TestCase testCase, ProcessResult runResult) {
        String actualOutput = normalize(runResult.stdout());
        String expectedOutput = normalize(testCase.expectedOutput());
        String status;
        String message;

        if (runResult.timedOut()) {
            status = "Time Limit Exceeded";
            message = "程序运行超时。";
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

    private ProcessResult runProcess(List<String> command, Path workDir, String input, Duration timeout)
            throws IOException {
        long started = System.nanoTime();
        Process process = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectErrorStream(false)
                .start();

        CompletableFuture<String> stdout = readAsync(process.getInputStream());
        CompletableFuture<String> stderr = readAsync(process.getErrorStream());

        if (input != null) {
            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(input.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        } else {
            process.getOutputStream().close();
        }

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new ProcessResult(-1, "", "评测线程被中断。", true, elapsedMs(started));
        }

        if (!finished) {
            process.destroyForcibly();
            return new ProcessResult(-1, futureValue(stdout), futureValue(stderr), true, elapsedMs(started));
        }

        return new ProcessResult(
                process.exitValue(),
                futureValue(stdout),
                futureValue(stderr),
                false,
                elapsedMs(started)
        );
    }

    private CompletableFuture<String> readAsync(java.io.InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (inputStream) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                return exception.getMessage();
            }
        });
    }

    private String futureValue(CompletableFuture<String> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException exception) {
            return "";
        }
    }

    private long elapsedMs(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
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

    private void deleteQuietly(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best effort cleanup for temporary judge files.
                }
            });
        } catch (IOException ignored) {
            // Best effort cleanup for temporary judge files.
        }
    }

    private record ProcessResult(
            int exitCode,
            String stdout,
            String stderr,
            boolean timedOut,
            long durationMs
    ) {
    }
}
