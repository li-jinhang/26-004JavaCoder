package com.example.javacoder.service.sandbox;

import java.io.IOException;

public interface SandboxRunner {

    SandboxSession createSession(String sourceCode, JudgeLimits limits) throws IOException;
}
