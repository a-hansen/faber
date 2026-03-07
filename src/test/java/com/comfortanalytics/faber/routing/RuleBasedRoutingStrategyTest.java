package com.comfortanalytics.faber.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.comfortanalytics.faber.orchestrator.TaskRequest;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleBasedRoutingStrategyTest {

    private final RuleBasedRoutingStrategy strategy = new RuleBasedRoutingStrategy();

    @Test
    void routesJavaRequestsToJavaDeveloper() {
        TaskRequest request = request("Please write a JAVA method to parse JSON.");

        assertEquals(AgentRole.JAVA_DEVELOPER, strategy.route(request));
    }

    @Test
    void routesFinanceRequestsToFinancialAnalyst() {
        TaskRequest request = request("Analyze this stock portfolio for market risk.");

        assertEquals(AgentRole.FINANCIAL_ANALYST, strategy.route(request));
    }

    @Test
    void fallsBackToJavaDeveloperWhenNoKeywordMatches() {
        TaskRequest request = request("Help me think through a generic workflow.");

        assertEquals(AgentRole.JAVA_DEVELOPER, strategy.route(request));
    }

    @Test
    void rejectsNullRequests() {
        assertThrows(NullPointerException.class, () -> strategy.route(null));
    }

    private static TaskRequest request(String userInput) {
        return new TaskRequest("req-1", userInput, Map.of(), Instant.parse("2026-03-07T00:00:00Z"));
    }
}

