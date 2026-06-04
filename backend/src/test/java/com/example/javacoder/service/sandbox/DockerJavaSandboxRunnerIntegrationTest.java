package com.example.javacoder.service.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.javacoder.service.LanguageRegistry;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DockerJavaSandboxRunnerIntegrationTest {

    @BeforeAll
    static void requireDockerSandboxImage() throws Exception {
        try {
            Process process = new ProcessBuilder(
                    "docker",
                    "image",
                    "inspect",
                    "javacoder-java17-sandbox:latest"
            ).redirectErrorStream(true).start();
            assumeTrue(process.waitFor() == 0, "javacoder-java17-sandbox:latest image is not available");
        } catch (IOException exception) {
            assumeTrue(false, "Docker CLI is not available");
        }
    }

    @Test
    void compilesAndRunsJavaCodeInsideRealDockerSandbox() throws Exception {
        JudgeSandboxProperties properties = new JudgeSandboxProperties();
        properties.setMode("docker");
        properties.setJavaImage("javacoder-java17-sandbox:latest");
        properties.setMemoryMb(256);
        properties.setCpus("1.0");
        properties.setPidsLimit(64);
        properties.setTmpfsMb(64);
        properties.setNetworkEnabled(false);

        DockerJavaSandboxRunner runner = new DockerJavaSandboxRunner(properties);
        JudgeLimits limits = new JudgeLimits(Duration.ofSeconds(8), Duration.ofSeconds(3), 65536, 65536);
        LanguageSpec java = new LanguageRegistry(properties).findById("java").orElseThrow();

        try (SandboxSession session = runner.createSession(
                java,
                """
                import java.util.Scanner;

                public class Main {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        System.out.println(scanner.nextInt() + scanner.nextInt());
                    }
                }
                """,
                limits
        )) {
            SandboxProcessResult compileResult = session.compile();
            SandboxProcessResult runResult = session.run("2 40\n");

            assertThat(compileResult.exitCode())
                    .withFailMessage("compile stderr: %s%ncompile stdout: %s", compileResult.stderr(), compileResult.stdout())
                    .isZero();
            assertThat(compileResult.timedOut()).isFalse();
            assertThat(runResult.exitCode())
                    .withFailMessage("run stderr: %s%nrun stdout: %s", runResult.stderr(), runResult.stdout())
                    .isZero();
            assertThat(runResult.stdout().trim()).isEqualTo("42");
            assertThat(runResult.timedOut()).isFalse();
            assertThat(runResult.outputLimitExceeded()).isFalse();
        }
    }
}
