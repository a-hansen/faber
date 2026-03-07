package com.comfortanalytics.faber.audit;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.time.Instant;
import java.util.Objects;

public record ChatTranscriptEntry(
        @Nonnull Instant timestamp,
        @Nonnull String agent,
        @Nonnull String prompt,
        @Nonnull String response) {

    public ChatTranscriptEntry {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(response, "response");
    }
}

