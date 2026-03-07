package com.comfortanalytics.faber.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.comfortanalytics.faber.routing.AgentRole;
import com.comfortanalytics.faber.routing.RuleBasedRoutingStrategy;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrchestratorAgentTest {

    @Test
    void routesRequestsThroughTheConfiguredStrategy() {
        OrchestratorAgent orchestrator = new OrchestratorAgent(new RuleBasedRoutingStrategy());
        TaskRequest request = new TaskRequest(
                "req-1",
                "Analyze this stock portfolio for earnings risk.",
                Map.of(),
                Instant.parse("2026-03-07T00:00:00Z"));

        assertEquals(AgentRole.FINANCIAL_ANALYST, orchestrator.route(request));
    }

    @Test
    void rejectsNullRoutingStrategy() {
        assertThrows(NullPointerException.class, () -> new OrchestratorAgent(null));
    }

    @Test
    void rejectsNullRequests() {
        OrchestratorAgent orchestrator = new OrchestratorAgent(new RuleBasedRoutingStrategy());

        assertThrows(NullPointerException.class, () -> orchestrator.route(null));
    }
}
