package com.comfortanalytics.faber.routing;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.orchestrator.TaskRequest;

public interface RoutingStrategy {

    @Nonnull
    AgentRole route(@Nonnull TaskRequest request);
}
