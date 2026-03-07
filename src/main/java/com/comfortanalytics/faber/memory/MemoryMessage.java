package com.comfortanalytics.faber.memory;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.Objects;

public record MemoryMessage(
        @Nonnull String role,
        @Nonnull String text) {

    public MemoryMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(text, "text");
    }
}

