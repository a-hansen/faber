package com.comfortanalytics.faber.routing;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import java.util.Objects;

public record RoutingDecision(
        @Nonnull AgentRole role,
        @Nonnull ModelTier modelTier) {

    public RoutingDecision {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(modelTier, "modelTier");
    }
}

