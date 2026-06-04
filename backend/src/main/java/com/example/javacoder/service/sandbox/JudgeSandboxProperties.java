package com.example.javacoder.service.sandbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "judge.sandbox")
public class JudgeSandboxProperties {

    private String mode = "docker";
    private String dockerExecutable = "docker";
    private String javaImage = "javacoder-java17-sandbox:latest";
    private String pythonImage = "javacoder-python3-sandbox:latest";
    private String pythonExecutable = "python3";
    private Duration compileTimeout = Duration.ofSeconds(8);
    private Duration runTimeout = Duration.ofSeconds(3);
    private int maxSourceBytes = 65536;
    private int maxOutputBytes = 65536;
    private int memoryMb = 256;
    private String cpus = "1.0";
    private int pidsLimit = 64;
    private int tmpfsMb = 64;
    private boolean networkEnabled = false;

    public JudgeLimits limits() {
        return new JudgeLimits(compileTimeout, runTimeout, maxSourceBytes, maxOutputBytes);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDockerExecutable() {
        return dockerExecutable;
    }

    public void setDockerExecutable(String dockerExecutable) {
        this.dockerExecutable = dockerExecutable;
    }

    public String getJavaImage() {
        return javaImage;
    }

    public void setJavaImage(String javaImage) {
        this.javaImage = javaImage;
    }

    public String getPythonImage() {
        return pythonImage;
    }

    public void setPythonImage(String pythonImage) {
        this.pythonImage = pythonImage;
    }

    public String getPythonExecutable() {
        return pythonExecutable;
    }

    public void setPythonExecutable(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }

    public Duration getCompileTimeout() {
        return compileTimeout;
    }

    public void setCompileTimeout(Duration compileTimeout) {
        this.compileTimeout = compileTimeout;
    }

    public Duration getRunTimeout() {
        return runTimeout;
    }

    public void setRunTimeout(Duration runTimeout) {
        this.runTimeout = runTimeout;
    }

    public int getMaxSourceBytes() {
        return maxSourceBytes;
    }

    public void setMaxSourceBytes(int maxSourceBytes) {
        this.maxSourceBytes = maxSourceBytes;
    }

    public int getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(int maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    public int getMemoryMb() {
        return memoryMb;
    }

    public void setMemoryMb(int memoryMb) {
        this.memoryMb = memoryMb;
    }

    public String getCpus() {
        return cpus;
    }

    public void setCpus(String cpus) {
        this.cpus = cpus;
    }

    public int getPidsLimit() {
        return pidsLimit;
    }

    public void setPidsLimit(int pidsLimit) {
        this.pidsLimit = pidsLimit;
    }

    public int getTmpfsMb() {
        return tmpfsMb;
    }

    public void setTmpfsMb(int tmpfsMb) {
        this.tmpfsMb = tmpfsMb;
    }

    public boolean isNetworkEnabled() {
        return networkEnabled;
    }

    public void setNetworkEnabled(boolean networkEnabled) {
        this.networkEnabled = networkEnabled;
    }
}
