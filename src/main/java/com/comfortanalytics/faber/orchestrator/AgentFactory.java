package com.comfortanalytics.faber.orchestrator;

import com.comfortanalytics.faber.agents.BaseAgent;
import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.routing.RoutingDecision;

@FunctionalInterface
public interface AgentFactory {

    @Nonnull
    BaseAgent create(@Nonnull RoutingDecision decision);
}
