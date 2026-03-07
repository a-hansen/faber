package com.comfortanalytics.faber.model;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.Objects;

public record ModelProviderConfig(
        @Nonnull String providerId,
        @Nonnull String modelName) {

    public ModelProviderConfig {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(modelName, "modelName");
    }
}

