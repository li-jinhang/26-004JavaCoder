package com.example.javacoder.service.sandbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "judge.sandbox", name = "mode", havingValue = "local")
public class LocalJavaSandboxRunner implements SandboxRunner {

    @Override
    public SandboxSession createSession(LanguageSpec language, String sourceCode, JudgeLimits limits) throws IOException {
        Path workDir = Files.createTempDirectory("javacoder-local-judge-");
        Files.writeString(workDir.resolve(language.sourceFile()), sourceCode, StandardCharsets.UTF_8);
        return new LocalSession(language, workDir, limits);
    }

    private static class LocalSession implements SandboxSession {

        private final LanguageSpec language;
        private final Path workDir;
        private final JudgeLimits limits;

        LocalSession(LanguageSpec language, Path workDir, JudgeLimits limits) {
            this.language = language;
            this.workDir = workDir;
            this.limits = limits;
        }

        @Override
        public SandboxProcessResult compile() throws IOException {
            return ProcessSupport.runProcess(language.localCompileCommand(workDir), workDir, null, limits.compileTimeout(), limits.maxOutputBytes());
        }

        @Override
        public SandboxProcessResult run(String input) throws IOException {
            return ProcessSupport.runProcess(language.localRunCommand(workDir), workDir, input, limits.runTimeout(), limits.maxOutputBytes());
        }

        @Override
        public void close() {
            FileCleanup.deleteQuietly(workDir);
        }
    }
}
