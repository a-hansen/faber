package com.comfortanalytics.faber.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MemoryManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void appendsMessagesWithoutSummarizingWhenBelowThreshold() throws IOException, TimeoutException {
        PersistentChatMemoryStore store = new PersistentChatMemoryStore(tempDir.resolve("memory"));
        FakeMemoryCondenser condenser = new FakeMemoryCondenser(new MemoryMessage("assistant", "summary"));
        MemoryManager manager = new MemoryManager(
                store,
                new MemoryConfig(20, 0.8),
                messages -> messages.size() * 5,
                condenser);

        manager.append("java-memory", new MemoryMessage("user", "one"));
        List<MemoryMessage> storedMessages = manager.append("java-memory", new MemoryMessage("assistant", "two"));

        assertEquals(List.of(
                new MemoryMessage("user", "one"),
                new MemoryMessage("assistant", "two")), storedMessages);
        assertEquals(0, condenser.calls());
    }

    @Test
    void summarizesTheOldestSeventyPercentOfMessagesWhenThresholdIsExceeded()
            throws IOException, TimeoutException {
        PersistentChatMemoryStore store = new PersistentChatMemoryStore(tempDir.resolve("memory"));
        FakeMemoryCondenser condenser = new FakeMemoryCondenser(new MemoryMessage("assistant", "summary"));
        MemoryManager manager = new MemoryManager(
                store,
                new MemoryConfig(25, 0.8),
                messages -> messages.size() * 5,
                condenser);

        manager.append("finance-memory", new MemoryMessage("user", "m1"));
        manager.append("finance-memory", new MemoryMessage("assistant", "m2"));
        manager.append("finance-memory", new MemoryMessage("user", "m3"));
        manager.append("finance-memory", new MemoryMessage("assistant", "m4"));
        List<MemoryMessage> storedMessages = manager.append("finance-memory", new MemoryMessage("user", "m5"));

        assertEquals(1, condenser.calls());
        assertEquals(List.of(
                new MemoryMessage("user", "m1"),
                new MemoryMessage("assistant", "m2"),
                new MemoryMessage("user", "m3")), condenser.lastMessages());
        assertEquals(List.of(
                new MemoryMessage("assistant", "summary"),
                new MemoryMessage("assistant", "m4"),
                new MemoryMessage("user", "m5")), storedMessages);
        assertEquals(storedMessages, store.load("finance-memory"));
    }

    @Test
    void rejectsNullMessages() {
        PersistentChatMemoryStore store = new PersistentChatMemoryStore(tempDir.resolve("memory"));
        MemoryManager manager = new MemoryManager(
                store,
                new MemoryConfig(25, 0.8),
                messages -> messages.size() * 5,
                messages -> new MemoryMessage("assistant", "summary"));

        assertThrows(NullPointerException.class, () -> manager.append("finance-memory", null));
    }

    private static final class FakeMemoryCondenser implements MemoryCondenser {

        private final MemoryMessage summary;
        private List<MemoryMessage> lastMessages = List.of();
        private int calls;

        private FakeMemoryCondenser(MemoryMessage summary) {
            this.summary = summary;
        }

        @Override
        public MemoryMessage condense(List<MemoryMessage> messages) {
            calls++;
            lastMessages = List.copyOf(messages);
            return summary;
        }

        int calls() {
            return calls;
        }

        List<MemoryMessage> lastMessages() {
            return lastMessages;
        }
    }
}

