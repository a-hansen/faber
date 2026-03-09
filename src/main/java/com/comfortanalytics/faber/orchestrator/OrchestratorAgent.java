package com.comfortanalytics.faber.orchestrator;

import com.comfortanalytics.faber.agents.BaseAgent;
import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.routing.AgentRole;
import com.comfortanalytics.faber.routing.DynamicRoutingStrategy;
import com.comfortanalytics.faber.routing.RoutingDecision;
import com.comfortanalytics.faber.routing.RoutingStrategy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class OrchestratorAgent {

    private static final ModelTier DEFAULT_MODEL_TIER = ModelTier.TIER2_BALANCED;
    private static final String JAVA_DEVELOPER_PERSONA =
            "You are a Java developer agent. Focus on correctness, maintainable Java code, and practical implementation details.";
    private static final String FINANCIAL_ANALYST_PERSONA =
            "You are a financial analyst agent. Focus on risk, market context, and clear financial reasoning.";
    private static final String CONTEXT_CONDENSER_PERSONA =
            "You are a context condenser agent. Produce a concise memory summary that preserves key facts, decisions, and open questions.";

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
        BaseAgent agent = agentFactory.create(decision);
        return agent.handle(nonNullRequest, decision.modelTier());
    }

    @Nonnull
    private RoutingDecision selectDecision(TaskRequest request) {
        if (routingStrategy instanceof DynamicRoutingStrategy dynamicRoutingStrategy) {
            return dynamicRoutingStrategy.routeDecision(request);
        }
        return defaultDecision(routingStrategy.route(request));
    }

    @Nonnull
    private RoutingDecision defaultDecision(@Nonnull AgentRole role) {
        AgentRole nonNullRole = Objects.requireNonNull(role, "role");
        return switch (nonNullRole) {
            case JAVA_DEVELOPER -> new RoutingDecision(
                    AgentRole.JAVA_DEVELOPER,
                    DEFAULT_MODEL_TIER,
                    JAVA_DEVELOPER_PERSONA,
                    List.of("file_system", "gradle"),
                    List.of("code_map", "workspace_index"));
            case FINANCIAL_ANALYST -> new RoutingDecision(
                    AgentRole.FINANCIAL_ANALYST,
                    DEFAULT_MODEL_TIER,
                    FINANCIAL_ANALYST_PERSONA,
                    List.of(),
                    List.of());
            case CONTEXT_CONDENSER -> new RoutingDecision(
                    AgentRole.CONTEXT_CONDENSER,
                    DEFAULT_MODEL_TIER,
                    CONTEXT_CONDENSER_PERSONA,
                    List.of(),
                    List.of());
        };
    }
}
