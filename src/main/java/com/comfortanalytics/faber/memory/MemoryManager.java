package com.comfortanalytics.faber.memory;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class MemoryManager {

    private static final double SUMMARY_PORTION = 0.7;
    private static final int MAX_SUMMARIZATION_PASSES = 3;

    private final PersistentChatMemoryStore memoryStore;
    private final MemoryConfig memoryConfig;
    private final MemoryTokenEstimator tokenEstimator;
    private final MemoryCondenser memoryCondenser;

    public MemoryManager(
            @Nonnull PersistentChatMemoryStore memoryStore,
            @Nonnull MemoryConfig memoryConfig,
            @Nonnull MemoryTokenEstimator tokenEstimator,
            @Nonnull MemoryCondenser memoryCondenser) {
        this.memoryStore = Objects.requireNonNull(memoryStore, "memoryStore");
        this.memoryConfig = Objects.requireNonNull(memoryConfig, "memoryConfig");
        this.tokenEstimator = Objects.requireNonNull(tokenEstimator, "tokenEstimator");
        this.memoryCondenser = Objects.requireNonNull(memoryCondenser, "memoryCondenser");
    }

    @Nonnull
    public List<MemoryMessage> load(@Nonnull String memoryId) throws IOException {
        return memoryStore.load(Objects.requireNonNull(memoryId, "memoryId"));
    }

    @Nonnull
    public List<MemoryMessage> append(@Nonnull String memoryId, @Nonnull MemoryMessage message)
            throws IOException, TimeoutException {
        ArrayList<MemoryMessage> messages = new ArrayList<>(memoryStore.load(Objects.requireNonNull(memoryId, "memoryId")));
        messages.add(Objects.requireNonNull(message, "message"));

        // Condense the oldest messages when the estimated token threshold is exceeded.
        messages = summarizeIfNeeded(messages);
        memoryStore.save(memoryId, messages);
        return List.copyOf(messages);
    }

    private ArrayList<MemoryMessage> summarizeIfNeeded(ArrayList<MemoryMessage> messages) throws TimeoutException {
        for (int pass = 0; pass < MAX_SUMMARIZATION_PASSES; pass++) {
            if (!shouldSummarize(messages) || messages.size() < 2) {
                return messages;
            }

            // Replace the oldest 70% of messages with a single condensed summary.
            int summaryCount = summaryCount(messages.size());
            List<MemoryMessage> messagesToCondense = List.copyOf(messages.subList(0, summaryCount));
            List<MemoryMessage> recentMessages = List.copyOf(messages.subList(summaryCount, messages.size()));
            MemoryMessage summary = memoryCondenser.condense(messagesToCondense);

            ArrayList<MemoryMessage> condensedMessages = new ArrayList<>(recentMessages.size() + 1);
            condensedMessages.add(summary);
            condensedMessages.addAll(recentMessages);
            messages = condensedMessages;
        }
        return messages;
    }

    private boolean shouldSummarize(List<MemoryMessage> messages) {
        return tokenEstimator.estimate(messages) > memoryConfig.tokenLimit() * memoryConfig.summarizeThreshold();
    }

    private static int summaryCount(int messageCount) {
        int count = (int) Math.floor(messageCount * SUMMARY_PORTION);
        return Math.max(1, Math.min(messageCount - 1, count));
    }
}

