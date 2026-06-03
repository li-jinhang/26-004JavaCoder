package com.example.javacoder.service.sandbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "judge.sandbox", name = "mode", havingValue = "docker", matchIfMissing = true)
public class DockerJavaSandboxRunner implements SandboxRunner {

    private static final String UTF_8 = StandardCharsets.UTF_8.name();
    private final JudgeSandboxProperties properties;

    public DockerJavaSandboxRunner(JudgeSandboxProperties properties) {
        this.properties = properties;
    }

    @Override
    public SandboxSession createSession(String sourceCode, JudgeLimits limits) throws IOException {
        Path workDir = Files.createTempDirectory("javacoder-docker-judge-");
        allowSandboxUserToWrite(workDir);
        Files.writeString(workDir.resolve("Main.java"), sourceCode, StandardCharsets.UTF_8);
        return new DockerSession(workDir, limits, properties);
    }

    private void allowSandboxUserToWrite(Path workDir) {
        try {
            Set<PosixFilePermission> permissions = EnumSet.allOf(PosixFilePermission.class);
            Files.setPosixFilePermissions(workDir, permissions);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows and some filesystems do not expose POSIX permissions.
        }
    }

    private static class DockerSession implements SandboxSession {

        private final Path workDir;
        private final JudgeLimits limits;
        private final JudgeSandboxProperties properties;
        private final String sessionId = UUID.randomUUID().toString().replace("-", "");
        private final AtomicInteger runId = new AtomicInteger();
        private final List<String> containerNames = new CopyOnWriteArrayList<>();

        DockerSession(Path workDir, JudgeLimits limits, JudgeSandboxProperties properties) {
            this.workDir = workDir;
            this.limits = limits;
            this.properties = properties;
        }

        @Override
        public SandboxProcessResult compile() throws IOException {
            return runDockerCommand(compileCommand(), null, limits.compileTimeout());
        }

        @Override
        public SandboxProcessResult run(String input) throws IOException {
            return runDockerCommand(runCommand(), input, limits.runTimeout());
        }

        @Override
        public void close() {
            containerNames.forEach(this::removeContainerQuietly);
            FileCleanup.deleteQuietly(workDir);
        }

        private SandboxProcessResult runDockerCommand(
                List<String> sandboxCommand,
                String input,
                java.time.Duration timeout
        ) throws IOException {
            String containerName = "javacoder-" + sessionId + "-" + runId.incrementAndGet();
            containerNames.add(containerName);
            SandboxProcessResult result = ProcessSupport.runProcess(
                    dockerCommand(containerName, sandboxCommand),
                    null,
                    input,
                    timeout,
                    limits.maxOutputBytes()
            );
            if (result.timedOut() || result.outputLimitExceeded()) {
                removeContainerQuietly(containerName);
            }
            return result;
        }

        private List<String> dockerCommand(String containerName, List<String> sandboxCommand) {
            List<String> command = new ArrayList<>();
            command.add(properties.getDockerExecutable());
            command.add("run");
            command.add("--rm");
            command.add("-i");
            command.add("--name");
            command.add(containerName);
            command.add("--cpus");
            command.add(properties.getCpus());
            command.add("--memory");
            command.add(properties.getMemoryMb() + "m");
            command.add("--memory-swap");
            command.add(properties.getMemoryMb() + "m");
            command.add("--pids-limit");
            command.add(String.valueOf(properties.getPidsLimit()));
            command.add("--ulimit");
            command.add("nofile=64:64");
            command.add("--ulimit");
            command.add("fsize=10485760:10485760");
            command.add("--ulimit");
            command.add("nproc=" + properties.getPidsLimit() + ":" + properties.getPidsLimit());
            command.add("--read-only");
            command.add("--tmpfs");
            command.add("/tmp:rw,noexec,nosuid,size=" + properties.getTmpfsMb() + "m");
            command.add("--cap-drop");
            command.add("ALL");
            command.add("--security-opt");
            command.add("no-new-privileges");
            command.add("--network");
            command.add(properties.isNetworkEnabled() ? "bridge" : "none");
            command.add("--user");
            command.add("10001:10001");
            command.add("-v");
            command.add(workDir.toAbsolutePath() + ":/workspace:rw");
            command.add("-w");
            command.add("/workspace");
            command.add(properties.getJavaImage());
            command.addAll(sandboxCommand);
            return command;
        }

        private void removeContainerQuietly(String containerName) {
            try {
                ProcessSupport.runProcess(
                        List.of(properties.getDockerExecutable(), "rm", "-f", containerName),
                        null,
                        null,
                        java.time.Duration.ofSeconds(2),
                        1024
                );
            } catch (IOException ignored) {
                // Best effort cleanup for containers left behind by timeout or output-limit termination.
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

        private List<String> runCommand() {
            return List.of(
                    "java",
                    "-Dfile.encoding=" + UTF_8,
                    "-Dsun.stdout.encoding=" + UTF_8,
                    "-Dsun.stderr.encoding=" + UTF_8,
                    "-cp", "/workspace",
                    "Main"
            );
        }
    }
}
