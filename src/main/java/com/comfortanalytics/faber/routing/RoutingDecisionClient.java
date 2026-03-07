package com.comfortanalytics.faber.routing;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.orchestrator.TaskRequest;

@FunctionalInterface
public interface RoutingDecisionClient {

    @Nonnull
    RoutingDecision routeDecision(@Nonnull TaskRequest request);
}

