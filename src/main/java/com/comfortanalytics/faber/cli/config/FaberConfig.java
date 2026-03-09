package com.comfortanalytics.faber.cli.config;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.Objects;

public record FaberConfig(
        @Nonnull WorkspaceConfig workspace,
        @Nonnull RoutingConfig routing,
        @Nonnull ModelsConfig models) {

    public FaberConfig {
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(routing, "routing");
        Objects.requireNonNull(models, "models");
    }
}

