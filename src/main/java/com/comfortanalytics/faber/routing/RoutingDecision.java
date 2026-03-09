package com.comfortanalytics.faber.routing;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import java.util.List;
import java.util.Objects;

public record RoutingDecision(
        @Nonnull AgentRole role,
        @Nonnull ModelTier modelTier,
        @Nonnull String persona,
        @Nonnull List<String> requiredTools,
        @Nonnull List<String> requiredContexts) {

    public RoutingDecision {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(modelTier, "modelTier");
        Objects.requireNonNull(persona, "persona");
        requiredTools = List.copyOf(Objects.requireNonNull(requiredTools, "requiredTools"));
        requiredContexts = List.copyOf(Objects.requireNonNull(requiredContexts, "requiredContexts"));
    }
}
