package com.comfortanalytics.faber.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.comfortanalytics.faber.model.ModelTier;
import org.junit.jupiter.api.Test;

class RoutingDecisionTest {

    @Test
    void rejectsNullComponents() {
        assertThrows(NullPointerException.class, () -> new RoutingDecision(null, ModelTier.TIER1_FAST));
        assertThrows(NullPointerException.class, () -> new RoutingDecision(AgentRole.JAVA_DEVELOPER, null));
    }

    @Test
    void exposesRoleAndModelTier() {
        RoutingDecision decision = new RoutingDecision(AgentRole.FINANCIAL_ANALYST, ModelTier.TIER3_POWERFUL);

        assertEquals(AgentRole.FINANCIAL_ANALYST, decision.role());
        assertEquals(ModelTier.TIER3_POWERFUL, decision.modelTier());
    }
}

