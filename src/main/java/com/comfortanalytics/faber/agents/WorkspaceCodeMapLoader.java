package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class WorkspaceCodeMapLoader {

    private final Path codeMapPath;

    public WorkspaceCodeMapLoader(@Nonnull Path workspaceRoot) {
        Path rootPath = Objects.requireNonNull(workspaceRoot, "workspaceRoot").toAbsolutePath().normalize();
        this.codeMapPath = rootPath.resolve("AI_CODE_MAP.md").normalize();
        if (!codeMapPath.startsWith(rootPath)) {
            throw new SecurityException("AI_CODE_MAP.md path escaped the workspace root");
        }
    }

    @Nonnull
    public String loadCodeMap() {
        try {
            if (!Files.exists(codeMapPath)) {
                throw new IllegalStateException("AI_CODE_MAP.md not found at: " + codeMapPath);
            }
            return Files.readString(codeMapPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read AI_CODE_MAP.md from: " + codeMapPath, e);
        }
    }
}

