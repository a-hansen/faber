package com.comfortanalytics.faber.orchestrator;

import com.comfortanalytics.faber.agents.BaseAgent;
import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.routing.AgentRole;
import com.comfortanalytics.faber.routing.DynamicRoutingStrategy;
import com.comfortanalytics.faber.routing.RoutingDecision;
import com.comfortanalytics.faber.routing.RoutingStrategy;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class OrchestratorAgent {

    private static final ModelTier DEFAULT_MODEL_TIER = ModelTier.TIER2_BALANCED;

    private final RoutingStrategy routingStrategy;
    private final AgentFactory agentFactory;

    public OrchestratorAgent(@Nonnull RoutingStrategy routingStrategy) {
        this.routingStrategy = Objects.requireNonNull(routingStrategy, "routingStrategy");
        this.agentFactory = null;
    }

    public OrchestratorAgent(@Nonnull RoutingStrategy routingStrategy, @Nonnull AgentFactory agentFactory) {
        this.routingStrategy = Objects.requireNonNull(routingStrategy, "routingStrategy");
        this.agentFactory = Objects.requireNonNull(agentFactory, "agentFactory");
    }

    @Nonnull
    public AgentRole route(@Nonnull TaskRequest request) {
        return routingStrategy.route(Objects.requireNonNull(request, "request"));
    }

    @Nonnull
    public String execute(@Nonnull TaskRequest request) throws TimeoutException {
        TaskRequest nonNullRequest = Objects.requireNonNull(request, "request");
        if (agentFactory == null) {
            throw new IllegalStateException("No AgentFactory configured for execution");
        }

        // Select a typed routing decision, then instantiate and execute the target agent.
        RoutingDecision decision = selectDecision(nonNullRequest);
        BaseAgent agent = agentFactory.create(decision.role());
        return agent.handle(nonNullRequest, decision.modelTier());
    }

    @Nonnull
    private RoutingDecision selectDecision(TaskRequest request) {
        if (routingStrategy instanceof DynamicRoutingStrategy dynamicRoutingStrategy) {
            return dynamicRoutingStrategy.routeDecision(request);
        }
        return new RoutingDecision(routingStrategy.route(request), DEFAULT_MODEL_TIER);
    }
}
