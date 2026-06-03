package com.example.javacoder.service.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DockerJavaSandboxRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsDockerCommandWithIsolationFlags() throws Exception {
        Path fakeDocker = createFakeDocker();
        JudgeSandboxProperties properties = new JudgeSandboxProperties();
        properties.setDockerExecutable(fakeDocker.toString());
        properties.setJavaImage("test-java-sandbox:latest");
        properties.setNetworkEnabled(false);
        properties.setMemoryMb(128);
        properties.setCpus("0.5");
        properties.setPidsLimit(16);
        properties.setTmpfsMb(32);

        DockerJavaSandboxRunner runner = new DockerJavaSandboxRunner(properties);
        JudgeLimits limits = new JudgeLimits(Duration.ofSeconds(8), Duration.ofSeconds(3), 65536, 65536);

        try (SandboxSession session = runner.createSession(
                """
                import java.util.Scanner;

                public class Main {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        System.out.println(scanner.nextLine().toUpperCase());
                    }
                }
                """,
                limits
        )) {
            SandboxProcessResult compileResult = session.compile();
            SandboxProcessResult runResult = session.run("sandbox\n");

            assertThat(compileResult.exitCode()).isZero();
            assertThat(runResult.exitCode()).isZero();
            assertThat(runResult.stdout().trim()).isEqualTo("SANDBOX");
        }

        String commandLog = Files.readString(tempDir.resolve("docker-commands.log"));
        assertThat(commandLog).contains("run --rm -i");
        assertThat(commandLog).contains("--network none");
        assertThat(commandLog).contains("--cap-drop ALL");
        assertThat(commandLog).contains("--security-opt no-new-privileges");
        assertThat(commandLog).contains("--read-only");
        assertThat(commandLog).contains("--tmpfs /tmp:rw,noexec,nosuid,size=32m");
        assertThat(commandLog).contains("--memory 128m");
        assertThat(commandLog).contains("--memory-swap 128m");
        assertThat(commandLog).contains("--cpus 0.5");
        assertThat(commandLog).contains("--pids-limit 16");
        assertThat(commandLog).contains("--ulimit nofile=64:64");
        assertThat(commandLog).contains("--ulimit fsize=10485760:10485760");
        assertThat(commandLog).contains("--ulimit nproc=16:16");
        assertThat(commandLog).contains("--user 10001:10001");
        assertThat(commandLog).contains("-w /workspace");
    }

    private Path createFakeDocker() throws Exception {
        Path fakeDocker = tempDir.resolve(isWindows() ? "docker.cmd" : "docker");
        Path commandLog = tempDir.resolve("docker-commands.log");
        if (isWindows()) {
            Files.writeString(fakeDocker, """
                    @echo off
                    echo %%* >> "%s"
                    if "%%1"=="rm" exit /b 0
                    echo SANDBOX
                    exit /b 0
                    """.formatted(commandLog));
        } else {
            Files.writeString(fakeDocker, """
                    #!/usr/bin/env sh
                    printf '%s\\n' "$*" >> '%s'
                    if [ "$1" = "rm" ]; then
                      exit 0
                    fi
                    printf 'SANDBOX\\n'
                    """.formatted(commandLog));
            fakeDocker.toFile().setExecutable(true);
        }
        return fakeDocker;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
