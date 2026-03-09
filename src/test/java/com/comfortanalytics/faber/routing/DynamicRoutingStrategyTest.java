package com.comfortanalytics.faber.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DynamicRoutingStrategyTest {

    @Test
    void routesUsingTheStructuredRoutingDecision() {
        DynamicRoutingStrategy strategy = new DynamicRoutingStrategy(
                request -> new RoutingDecision(
                        AgentRole.FINANCIAL_ANALYST,
                        ModelTier.TIER3_POWERFUL,
                        "Analyze the portfolio for risk.",
                        List.of(),
                        List.of()));
        TaskRequest request = request("Analyze this market scenario.");

        assertEquals(AgentRole.FINANCIAL_ANALYST, strategy.route(request));
        assertEquals(ModelTier.TIER3_POWERFUL, strategy.routeDecision(request).modelTier());
        assertEquals("Analyze the portfolio for risk.", strategy.routeDecision(request).persona());
    }

    @Test
    void fallsBackToRuleBasedRoutingWhenTheDecisionClientFails() {
        DynamicRoutingStrategy strategy = new DynamicRoutingStrategy(
                request -> {
                    throw new IllegalStateException("tier-1 model unavailable");
                });
        TaskRequest request = request("Write a Java method to parse JSON.");

        RoutingDecision decision = strategy.routeDecision(request);

        assertEquals(AgentRole.JAVA_DEVELOPER, decision.role());
        assertEquals(ModelTier.TIER2_BALANCED, decision.modelTier());
        assertEquals(List.of("file_system", "gradle"), decision.requiredTools());
        assertEquals(List.of("code_map", "workspace_index"), decision.requiredContexts());
    }

    @Test
    void rejectsNullRequests() {
        DynamicRoutingStrategy strategy = new DynamicRoutingStrategy(
                request -> new RoutingDecision(
                        AgentRole.JAVA_DEVELOPER,
                        ModelTier.TIER1_FAST,
                        "Write Java code carefully.",
                        List.of("file_system"),
                        List.of("code_map")));

        assertThrows(NullPointerException.class, () -> strategy.route(null));
        assertThrows(NullPointerException.class, () -> strategy.routeDecision(null));
    }

    private static TaskRequest request(String userInput) {
        return new TaskRequest("req-1", userInput, Map.of(), Instant.parse("2026-03-07T00:00:00Z"));
    }
}
