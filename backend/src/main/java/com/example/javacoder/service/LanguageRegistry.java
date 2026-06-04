package com.example.javacoder.service;

import com.example.javacoder.model.SupportedLanguage;
import com.example.javacoder.service.sandbox.JudgeSandboxProperties;
import com.example.javacoder.service.sandbox.LanguageSpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LanguageRegistry {

    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    private final List<LanguageSpec> languages;

    public LanguageRegistry(JudgeSandboxProperties properties) {
        this.languages = List.of(javaSpec(properties), pythonSpec(properties));
    }

    public List<SupportedLanguage> supportedLanguages() {
        return languages.stream()
                .map(language -> new SupportedLanguage(
                        language.id(),
                        language.displayName(),
                        language.defaultStarterCode()
                ))
                .toList();
    }

    public Optional<LanguageSpec> findById(String languageId) {
        if (languageId == null || languageId.isBlank()) {
            return findById("java");
        }
        String normalized = languageId.trim().toLowerCase(Locale.ROOT);
        return languages.stream()
                .filter(language -> language.id().equals(normalized))
                .findFirst();
    }

    private LanguageSpec javaSpec(JudgeSandboxProperties properties) {
        return new LanguageSpec(
                "java",
                "Java 17",
                "Main.java",
                properties.getJavaImage(),
                List.of(
                        "javac",
                        "-encoding", UTF_8,
                        "-J-Dfile.encoding=" + UTF_8,
                        "-J-Dsun.stdout.encoding=" + UTF_8,
                        "-J-Dsun.stderr.encoding=" + UTF_8,
                        "{sourceFile}"
                ),
                List.of(
                        "java",
                        "-Dfile.encoding=" + UTF_8,
                        "-Dsun.stdout.encoding=" + UTF_8,
                        "-Dsun.stderr.encoding=" + UTF_8,
                        "-cp", "{workspace}",
                        "Main"
                ),
                List.of(
                        "javac",
                        "-encoding", UTF_8,
                        "-J-Dfile.encoding=" + UTF_8,
                        "-J-Dsun.stdout.encoding=" + UTF_8,
                        "-J-Dsun.stderr.encoding=" + UTF_8,
                        "{sourceFile}"
                ),
                List.of(
                        "java",
                        "-Dfile.encoding=" + UTF_8,
                        "-Dsun.stdout.encoding=" + UTF_8,
                        "-Dsun.stderr.encoding=" + UTF_8,
                        "-cp", "{workspace}",
                        "Main"
                ),
                """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        // TODO: read input and print the answer
                    }
                }
                """
        );
    }

    private LanguageSpec pythonSpec(JudgeSandboxProperties properties) {
        return new LanguageSpec(
                "python",
                "Python 3",
                "main.py",
                properties.getPythonImage(),
                List.of(properties.getPythonExecutable(), "-m", "py_compile", "{sourceFile}"),
                List.of(properties.getPythonExecutable(), "{sourceFile}"),
                List.of("python3", "-m", "py_compile", "{sourceFile}"),
                List.of("python3", "{sourceFile}"),
                """
                import sys

                data = sys.stdin.read().strip().split()

                # TODO: parse input and print the answer
                """
        );
    }
}
