package com.comfortanalytics.faber.agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.comfortanalytics.faber.memory.MemoryMessage;
import com.comfortanalytics.faber.model.ModelProviderManager;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.routing.AgentRole;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class ContextCondenserAgentTest {

    @Test
    void exposesItsFixedRoleAndDefaultTier() {
        AgentTestSupport.CapturingModelProvider provider =
                new AgentTestSupport.CapturingModelProvider("gemini", "gemini-pro", "summary-response");
        ModelProviderManager manager = AgentTestSupport.managerForTier(ModelTier.TIER2_BALANCED, provider);
        ContextCondenserAgent agent = new ContextCondenserAgent(manager);

        assertEquals(AgentRole.CONTEXT_CONDENSER, agent.role());
        assertEquals(ModelTier.TIER2_BALANCED, agent.modelTier());
    }

    @Test
    void condensesMessagesIntoASummaryMemoryMessage() throws TimeoutException {
        AgentTestSupport.CapturingModelProvider provider =
                new AgentTestSupport.CapturingModelProvider("gemini", "gemini-pro", "condensed-summary");
        ModelProviderManager manager = AgentTestSupport.managerForTier(ModelTier.TIER2_BALANCED, provider);
        ContextCondenserAgent agent = new ContextCondenserAgent(manager);

        MemoryMessage summary = agent.condense(List.of(
                new MemoryMessage("user", "Need a JSON parser."),
                new MemoryMessage("assistant", "Jackson would work well.")));

        assertEquals(new MemoryMessage("assistant", "condensed-summary"), summary);
        assertTrue(provider.lastPrompt().contains("context condenser agent"));
        assertTrue(provider.lastPrompt().contains("user: Need a JSON parser."));
        assertTrue(provider.lastPrompt().contains("assistant: Jackson would work well."));
    }

    @Test
    void rejectsEmptyMessageLists() {
        AgentTestSupport.CapturingModelProvider provider =
                new AgentTestSupport.CapturingModelProvider("gemini", "gemini-pro", "condensed-summary");
        ModelProviderManager manager = AgentTestSupport.managerForTier(ModelTier.TIER2_BALANCED, provider);
        ContextCondenserAgent agent = new ContextCondenserAgent(manager);

        assertThrows(IllegalArgumentException.class, () -> agent.condense(List.of()));
    }
}

