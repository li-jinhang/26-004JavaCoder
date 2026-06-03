package com.example.javacoder.service.sandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

final class FileCleanup {

    private FileCleanup() {
    }

    static void deleteQuietly(Path root) {
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
}
