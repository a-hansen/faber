package com.comfortanalytics.faber.memory;

public record MemoryConfig(int tokenLimit, double summarizeThreshold) {

    public MemoryConfig {
        if (tokenLimit <= 0) {
            throw new IllegalArgumentException("tokenLimit must be greater than 0");
        }
        if (summarizeThreshold <= 0.0 || summarizeThreshold > 1.0) {
            throw new IllegalArgumentException("summarizeThreshold must be in the range (0.0, 1.0]");
        }
    }
}

