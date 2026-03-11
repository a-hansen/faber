package com.comfortanalytics.faber.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.comfortanalytics.faber.model.ModelTier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoutingDecisionTest {

    @Test
    void rejectsNullComponents() {
        assertThrows(NullPointerException.class, () -> new RoutingDecision(null, ModelTier.TIER1_FAST, "persona", List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new RoutingDecision(AgentRole.JAVA_DEVELOPER, null, "persona", List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new RoutingDecision(AgentRole.JAVA_DEVELOPER, ModelTier.TIER1_FAST, null, List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new RoutingDecision(AgentRole.JAVA_DEVELOPER, ModelTier.TIER1_FAST, "persona", null, List.of()));
        assertThrows(NullPointerException.class, () -> new RoutingDecision(AgentRole.JAVA_DEVELOPER, ModelTier.TIER1_FAST, "persona", List.of(), null));
    }

    @Test
    void exposesAllConfiguredFieldsAndDefensivelyCopiesLists() {
        List<String> requiredTools = new ArrayList<>(List.of("file_system", "gradle"));
        List<String> requiredContexts = new ArrayList<>(List.of("ai_code_map", "workspace_index"));
        RoutingDecision decision = new RoutingDecision(
                AgentRole.FINANCIAL_ANALYST,
                ModelTier.TIER3_POWERFUL,
                "Review the portfolio for risk.",
                requiredTools,
                requiredContexts);

        requiredTools.add("ignored");
        requiredContexts.add("ignored");

        assertEquals(AgentRole.FINANCIAL_ANALYST, decision.role());
        assertEquals(ModelTier.TIER3_POWERFUL, decision.modelTier());
        assertEquals("Review the portfolio for risk.", decision.persona());
        assertEquals(List.of("file_system", "gradle"), decision.requiredTools());
        assertEquals(List.of("ai_code_map", "workspace_index"), decision.requiredContexts());
    }
}
