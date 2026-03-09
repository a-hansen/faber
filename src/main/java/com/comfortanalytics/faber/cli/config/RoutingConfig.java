package com.comfortanalytics.faber.cli.config;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.Locale;
import java.util.Objects;

public record RoutingConfig(@Nonnull String mode) {

    public RoutingConfig {
        Objects.requireNonNull(mode, "mode");
        if (mode.isBlank()) {
            throw new IllegalArgumentException("mode must not be blank");
        }
        mode = mode.trim().toUpperCase(Locale.ROOT);
    }
}

