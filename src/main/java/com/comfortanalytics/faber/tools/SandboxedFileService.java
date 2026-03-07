package com.comfortanalytics.faber.tools;

import com.comfortanalytics.faber.annotation.Nonnull;
import dev.langchain4j.agent.tool.Tool;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class SandboxedFileService {

    private final Path rootPath;
    private final Mode mode;

    public SandboxedFileService(@Nonnull Path rootPath, @Nonnull Mode mode) {
        this.rootPath = Objects.requireNonNull(rootPath, "rootPath").toAbsolutePath().normalize();
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Tool("Read a text file from the sandboxed workspace.")
    @Nonnull
    public String readFile(@Nonnull String userPath) {
        Path resolvedPath = resolvePath(Objects.requireNonNull(userPath, "userPath"));
        try {
            return Files.readString(resolvedPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file: " + userPath, e);
        }
    }

    @Tool("Write a text file inside the sandboxed workspace.")
    @Nonnull
    public String writeFile(@Nonnull String userPath, @Nonnull String content) {
        if (mode == Mode.READ_ONLY) {
            throw new UnsupportedOperationException("Sandbox is read-only");
        }
        Path resolvedPath = resolvePath(Objects.requireNonNull(userPath, "userPath"));
        try {
            Path parent = resolvedPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(resolvedPath, Objects.requireNonNull(content, "content"), StandardCharsets.UTF_8);
            return "Wrote file: " + resolvedPath.getFileName();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write file: " + userPath, e);
        }
    }

    private Path resolvePath(String userPath) {
        Path resolvedPath = rootPath.resolve(userPath).toAbsolutePath().normalize();
        if (!resolvedPath.startsWith(rootPath)) {
            throw new SecurityException("Path traversal detected: " + userPath);
        }
        return resolvedPath;
    }

    public enum Mode {
        READ_ONLY,
        READ_WRITE
    }
}

