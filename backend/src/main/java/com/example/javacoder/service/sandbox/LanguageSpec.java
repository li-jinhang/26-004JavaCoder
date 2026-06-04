package com.example.javacoder.service.sandbox;

import java.nio.file.Path;
import java.util.List;

public record LanguageSpec(
        String id,
        String displayName,
        String sourceFile,
        String dockerImage,
        List<String> localCompileCommand,
        List<String> localRunCommand,
        List<String> sandboxCompileCommand,
        List<String> sandboxRunCommand,
        String defaultStarterCode
) {

    public List<String> localCompileCommand(Path workDir) {
        return render(localCompileCommand, workDir.toString());
    }

    public List<String> localRunCommand(Path workDir) {
        return render(localRunCommand, workDir.toString());
    }

    public List<String> sandboxCompileCommand() {
        return render(sandboxCompileCommand, "/workspace");
    }

    public List<String> sandboxRunCommand() {
        return render(sandboxRunCommand, "/workspace");
    }

    private List<String> render(List<String> command, String workspace) {
        return command.stream()
                .map(part -> part.replace("{workspace}", workspace).replace("{sourceFile}", sourceFile))
                .toList();
    }
}
