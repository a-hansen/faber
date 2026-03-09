package com.comfortanalytics.faber.cli.config;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.Objects;

public record WorkspaceConfig(@Nonnull String rootPath) {

    public WorkspaceConfig {
        Objects.requireNonNull(rootPath, "rootPath");
        if (rootPath.isBlank()) {
            throw new IllegalArgumentException("rootPath must not be blank");
        }
    }
}

