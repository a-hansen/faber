package com.comfortanalytics.faber.cli.config;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.Locale;
import java.util.Objects;

public record ProviderConfig(@Nonnull String provider, @Nonnull String model) {

    public ProviderConfig {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(model, "model");
        if (provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        provider = provider.trim().toLowerCase(Locale.ROOT);
        model = model.trim();
    }
}

