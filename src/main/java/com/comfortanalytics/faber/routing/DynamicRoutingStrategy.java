package com.comfortanalytics.faber.routing;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import java.util.List;
import java.util.Objects;

public final class DynamicRoutingStrategy implements RoutingStrategy {

    private static final ModelTier FALLBACK_MODEL_TIER = ModelTier.TIER2_BALANCED;
    private static final String JAVA_DEVELOPER_PERSONA =
            "You are a Java developer agent. Focus on correctness, maintainable Java code, and practical implementation details.";
    private static final String FINANCIAL_ANALYST_PERSONA =
            "You are a financial analyst agent. Focus on risk, market context, and clear financial reasoning.";
    private static final String CONTEXT_CONDENSER_PERSONA =
            "You are a context condenser agent. Produce a concise memory summary that preserves key facts, decisions, and open questions.";

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
            return fallbackDecision(fallbackStrategy.route(nonNullRequest));
        }
    }

    @Nonnull
    private RoutingDecision fallbackDecision(@Nonnull AgentRole role) {
        AgentRole nonNullRole = Objects.requireNonNull(role, "role");
        return switch (nonNullRole) {
            case JAVA_DEVELOPER -> new RoutingDecision(
                    AgentRole.JAVA_DEVELOPER,
                    FALLBACK_MODEL_TIER,
                    JAVA_DEVELOPER_PERSONA,
                    List.of("file_system", "gradle"),
                    List.of("ai_code_map", "workspace_index"));
            case FINANCIAL_ANALYST -> new RoutingDecision(
                    AgentRole.FINANCIAL_ANALYST,
                    FALLBACK_MODEL_TIER,
                    FINANCIAL_ANALYST_PERSONA,
                    List.of(),
                    List.of());
            case CONTEXT_CONDENSER -> new RoutingDecision(
                    AgentRole.CONTEXT_CONDENSER,
                    FALLBACK_MODEL_TIER,
                    CONTEXT_CONDENSER_PERSONA,
                    List.of(),
                    List.of());
        };
    }
}
