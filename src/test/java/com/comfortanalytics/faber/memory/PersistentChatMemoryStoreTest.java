package com.comfortanalytics.faber.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistentChatMemoryStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsMessagesAsPrettyJson() throws IOException {
        PersistentChatMemoryStore store = new PersistentChatMemoryStore(tempDir.resolve("memory"));
        List<MemoryMessage> messages = List.of(
                new MemoryMessage("user", "Write a Java method."),
                new MemoryMessage("assistant", "Here is a draft."));

        store.save("Faber_JavaDeveloper", messages);

        assertEquals(messages, store.load("Faber_JavaDeveloper"));
        String json = Files.readString(tempDir.resolve("memory").resolve("Faber_JavaDeveloper.json"));
        assertTrue(json.contains(System.lineSeparator()));
        assertTrue(json.contains("\"messages\""));
    }

    @Test
    void returnsAnEmptyListWhenTheMemoryFileDoesNotExist() throws IOException {
        PersistentChatMemoryStore store = new PersistentChatMemoryStore(tempDir.resolve("memory"));

        assertEquals(List.of(), store.load("missing-memory"));
    }

    @Test
    void rejectsPathTraversalMemoryIds() {
        PersistentChatMemoryStore store = new PersistentChatMemoryStore(tempDir.resolve("memory"));

        assertThrows(SecurityException.class, () -> store.save("../escape", List.of()));
    }
}

