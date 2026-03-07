package com.comfortanalytics.faber.agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.comfortanalytics.faber.model.ModelProviderManager;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import com.comfortanalytics.faber.routing.AgentRole;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class FinancialAnalystAgentTest {

    @Test
    void exposesItsFixedRoleAndDefaultTier() {
        AgentTestSupport.CapturingModelProvider provider =
                new AgentTestSupport.CapturingModelProvider("gemini", "gemini-pro", "agent-response");
        ModelProviderManager manager = AgentTestSupport.managerForTier(ModelTier.TIER2_BALANCED, provider);
        FinancialAnalystAgent agent = new FinancialAnalystAgent(manager);

        assertEquals(AgentRole.FINANCIAL_ANALYST, agent.role());
        assertEquals(ModelTier.TIER2_BALANCED, agent.modelTier());
    }

    @Test
    void buildsAFinanceFocusedPrompt() throws TimeoutException {
        AgentTestSupport.CapturingModelProvider provider =
                new AgentTestSupport.CapturingModelProvider("gemini", "gemini-pro", "finance-response");
        ModelProviderManager manager = AgentTestSupport.managerForTier(ModelTier.TIER2_BALANCED, provider);
        FinancialAnalystAgent agent = new FinancialAnalystAgent(manager);
        TaskRequest request = new TaskRequest(
                "req-1",
                "Analyze this stock portfolio for downside risk.",
                Map.of(),
                Instant.parse("2026-03-07T00:00:00Z"));

        assertEquals("finance-response", agent.handle(request));
        assertTrue(provider.lastPrompt().contains("financial analyst agent"));
        assertTrue(provider.lastPrompt().contains("clear financial reasoning"));
        assertTrue(provider.lastPrompt().contains("Analyze this stock portfolio for downside risk."));
    }
}
