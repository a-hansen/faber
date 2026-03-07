package com.comfortanalytics.faber.orchestrator;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class TaskRequest {

    private final String requestId;
    private final String userInput;
    private final Map<String, String> metadata;
    private final Instant timestamp;

    public TaskRequest(
            @Nonnull String requestId,
            @Nonnull String userInput,
            @Nonnull Map<String, String> metadata,
            @Nonnull Instant timestamp) {
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.userInput = Objects.requireNonNull(userInput, "userInput");
        this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    @Nonnull
    public String requestId() {
        return requestId;
    }

    @Nonnull
    public String userInput() {
        return userInput;
    }

    @Nonnull
    public Map<String, String> metadata() {
        return metadata;
    }

    @Nonnull
    public Instant timestamp() {
        return timestamp;
    }
}
