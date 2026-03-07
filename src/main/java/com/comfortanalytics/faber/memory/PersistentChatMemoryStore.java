package com.comfortanalytics.faber.memory;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class PersistentChatMemoryStore {

    private final Path rootPath;
    private final ObjectMapper objectMapper;

    public PersistentChatMemoryStore(@Nonnull Path rootPath) {
        this.rootPath = Objects.requireNonNull(rootPath, "rootPath").toAbsolutePath().normalize();
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Nonnull
    public List<MemoryMessage> load(@Nonnull String memoryId) throws IOException {
        Path file = resolveMemoryFile(Objects.requireNonNull(memoryId, "memoryId"));
        if (!Files.exists(file)) {
            return List.of();
        }
        StoredMemory storedMemory = objectMapper.readValue(file.toFile(), StoredMemory.class);
        if (storedMemory == null || storedMemory.messages() == null) {
            return List.of();
        }
        return List.copyOf(storedMemory.messages());
    }

    public void save(@Nonnull String memoryId, @Nonnull List<MemoryMessage> messages) throws IOException {
        Path file = resolveMemoryFile(Objects.requireNonNull(memoryId, "memoryId"));
        Files.createDirectories(rootPath);
        objectMapper.writeValue(file.toFile(), new StoredMemory(List.copyOf(Objects.requireNonNull(messages, "messages"))));
    }

    private Path resolveMemoryFile(String memoryId) {
        Path resolved = rootPath.resolve(memoryId + ".json").normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new SecurityException("Memory path traversal detected: " + memoryId);
        }
        return resolved;
    }

    private record StoredMemory(List<MemoryMessage> messages) {
    }
}

