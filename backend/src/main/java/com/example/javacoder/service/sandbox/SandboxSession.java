package com.example.javacoder.service.sandbox;

import java.io.IOException;

public interface SandboxSession extends AutoCloseable {

    SandboxProcessResult compile() throws IOException;

    SandboxProcessResult run(String input) throws IOException;

    @Override
    void close();
}
