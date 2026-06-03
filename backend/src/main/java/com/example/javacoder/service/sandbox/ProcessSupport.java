package com.example.javacoder.service.sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

final class ProcessSupport {

    private ProcessSupport() {
    }

    static SandboxProcessResult runProcess(
            List<String> command,
            Path workDir,
            String input,
            Duration timeout,
            int maxOutputBytes
    ) throws IOException {
        long started = System.nanoTime();
        ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(false);
        if (workDir != null) {
            processBuilder.directory(workDir.toFile());
        }
        Process process = processBuilder.start();
        AtomicBoolean outputLimitExceeded = new AtomicBoolean(false);

        CompletableFuture<StreamReadResult> stdout = readAsync(process.getInputStream(), maxOutputBytes, outputLimitExceeded, process);
        CompletableFuture<StreamReadResult> stderr = readAsync(process.getErrorStream(), maxOutputBytes, outputLimitExceeded, process);

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
            return new SandboxProcessResult(-1, "", "Judge thread interrupted.", true, false, false, elapsedMs(started));
        }

        if (!finished) {
            process.destroyForcibly();
            StreamReadResult out = futureValue(stdout);
            StreamReadResult err = futureValue(stderr);
            return new SandboxProcessResult(
                    -1,
                    out.text(),
                    err.text(),
                    true,
                    outputLimitExceeded.get() || out.limitExceeded() || err.limitExceeded(),
                    false,
                    elapsedMs(started)
            );
        }

        StreamReadResult out = futureValue(stdout);
        StreamReadResult err = futureValue(stderr);
        return new SandboxProcessResult(
                process.exitValue(),
                out.text(),
                err.text(),
                false,
                outputLimitExceeded.get() || out.limitExceeded() || err.limitExceeded(),
                isLikelyMemoryLimitExit(process.exitValue(), err.text()),
                elapsedMs(started)
        );
    }

    private static boolean isLikelyMemoryLimitExit(int exitCode, String stderr) {
        if (exitCode == 137) {
            return true;
        }
        if (stderr == null) {
            return false;
        }
        String lower = stderr.toLowerCase();
        return lower.contains("outofmemoryerror") || lower.contains("cannot allocate memory");
    }

    private static CompletableFuture<StreamReadResult> readAsync(
            InputStream inputStream,
            int maxBytes,
            AtomicBoolean outputLimitExceeded,
            Process process
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (inputStream) {
                return readLimited(inputStream, maxBytes, outputLimitExceeded, process);
            } catch (IOException exception) {
                return new StreamReadResult(exception.getMessage(), false);
            }
        });
    }

    private static StreamReadResult readLimited(
            InputStream inputStream,
            int maxBytes,
            AtomicBoolean outputLimitExceeded,
            Process process
    ) throws IOException {
        byte[] buffer = new byte[4096];
        byte[] output = new byte[Math.max(maxBytes, 0)];
        int stored = 0;
        boolean limitExceeded = false;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            int remaining = Math.max(maxBytes - stored, 0);
            if (remaining > 0) {
                int toCopy = Math.min(remaining, read);
                System.arraycopy(buffer, 0, output, stored, toCopy);
                stored += toCopy;
            }
            if (read > remaining) {
                limitExceeded = true;
                outputLimitExceeded.set(true);
                process.destroyForcibly();
            }
        }
        return new StreamReadResult(new String(output, 0, stored, StandardCharsets.UTF_8), limitExceeded);
    }

    private static StreamReadResult futureValue(CompletableFuture<StreamReadResult> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new StreamReadResult("", false);
        } catch (ExecutionException | TimeoutException exception) {
            return new StreamReadResult("", false);
        }
    }

    private static long elapsedMs(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private record StreamReadResult(String text, boolean limitExceeded) {
    }
}
