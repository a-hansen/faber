package com.comfortanalytics.faber.routing;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import java.util.Objects;

public final class DynamicRoutingStrategy implements RoutingStrategy {

    private static final ModelTier FALLBACK_MODEL_TIER = ModelTier.TIER2_BALANCED;

    private final RoutingDecisionClient routingDecisionClient;
    private final RoutingStrategy fallbackStrategy;

    public DynamicRoutingStrategy(@Nonnull RoutingDecisionClient routingDecisionClient) {
        this(routingDecisionClient, new RuleBasedRoutingStrategy());
    }

    DynamicRoutingStrategy(
            @Nonnull RoutingDecisionClient routingDecisionClient,
            @Nonnull RoutingStrategy fallbackStrategy) {
        this.routingDecisionClient = Objects.requireNonNull(routingDecisionClient, "routingDecisionClient");
        this.fallbackStrategy = Objects.requireNonNull(fallbackStrategy, "fallbackStrategy");
    }

    @Override
    @Nonnull
    public AgentRole route(@Nonnull TaskRequest request) {
        return routeDecision(request).role();
    }

    @Nonnull
    public RoutingDecision routeDecision(@Nonnull TaskRequest request) {
        TaskRequest nonNullRequest = Objects.requireNonNull(request, "request");
        try {
            RoutingDecision decision = routingDecisionClient.routeDecision(nonNullRequest);
            return Objects.requireNonNull(decision, "routingDecisionClient returned null");
        } catch (RuntimeException e) {
            return new RoutingDecision(fallbackStrategy.route(nonNullRequest), FALLBACK_MODEL_TIER);
        }
    }
}

