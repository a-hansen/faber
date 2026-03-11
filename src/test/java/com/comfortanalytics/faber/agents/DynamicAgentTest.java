package com.comfortanalytics.faber.agents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import com.comfortanalytics.faber.routing.AgentRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
class DynamicAgentTest {
    @Test
    void exposesTheConfiguredRoleAndModelTier() {
        DynamicAgent agent = new DynamicAgent(
                AgentRole.JAVA_DEVELOPER,
                ModelTier.TIER2_BALANCED,
                "Write maintainable Java code.",
                new RecordingExecutionEngine("ok"),
                List.of(),
                "");
        assertEquals(AgentRole.JAVA_DEVELOPER, agent.role());
        assertEquals(ModelTier.TIER2_BALANCED, agent.modelTier());
    }
    @Test
    void buildsSystemMessagesFromTheConfiguredPersonaToolsAndInjectedContext() throws Exception {
        RecordingExecutionEngine executionEngine = new RecordingExecutionEngine("agent-response");
        DynamicAgent agent = new DynamicAgent(
                AgentRole.JAVA_DEVELOPER,
                ModelTier.TIER3_POWERFUL,
                "You are a Java developer agent focused on precise refactors.",
                executionEngine,
                List.of("tool-a", "tool-b"),
                "Current codebase architecture:\n# AI_CODE_MAP\n\nCurrent workspace map:\npkg com.example");
        TaskRequest request = new TaskRequest(
                "req-1",
                "Refactor this service.",
                Map.of(),
                Instant.parse("2026-03-07T00:00:00Z"));
        assertEquals("agent-response", agent.handle(request));
        assertEquals(ModelTier.TIER3_POWERFUL, executionEngine.lastRequest().modelTier());
        assertEquals(List.of("tool-a", "tool-b"), executionEngine.lastRequest().tools());
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Agent role: JAVA_DEVELOPER"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Instructions: You are a Java developer agent focused on precise refactors."));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Injected context:"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Current codebase architecture:"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("pkg com.example"));
        assertEquals("Refactor this service.", executionEngine.lastRequest().userMessage());
    }
    private static final class RecordingExecutionEngine implements AgentExecutionEngine {
        private final String response;
        private AgentExecutionRequest lastRequest;
        private RecordingExecutionEngine(String response) {
            this.response = response;
        }
        @Override
        public String execute(AgentExecutionRequest request) {
            lastRequest = request;
            return response;
        }
        private AgentExecutionRequest lastRequest() {
            return lastRequest;
        }
    }
}
