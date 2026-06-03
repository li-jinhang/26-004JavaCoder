package com.example.javacoder.service.sandbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "judge.sandbox", name = "mode", havingValue = "local")
public class LocalJavaSandboxRunner implements SandboxRunner {

    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    @Override
    public SandboxSession createSession(String sourceCode, JudgeLimits limits) throws IOException {
        Path workDir = Files.createTempDirectory("javacoder-local-judge-");
        Files.writeString(workDir.resolve("Main.java"), sourceCode, StandardCharsets.UTF_8);
        return new LocalSession(workDir, limits);
    }

    private static class LocalSession implements SandboxSession {

        private final Path workDir;
        private final JudgeLimits limits;

        LocalSession(Path workDir, JudgeLimits limits) {
            this.workDir = workDir;
            this.limits = limits;
        }

        @Override
        public SandboxProcessResult compile() throws IOException {
            return ProcessSupport.runProcess(compileCommand(), workDir, null, limits.compileTimeout(), limits.maxOutputBytes());
        }

        @Override
        public SandboxProcessResult run(String input) throws IOException {
            return ProcessSupport.runProcess(runCommand(workDir), workDir, input, limits.runTimeout(), limits.maxOutputBytes());
        }

        @Override
        public void close() {
            FileCleanup.deleteQuietly(workDir);
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
    }
}
