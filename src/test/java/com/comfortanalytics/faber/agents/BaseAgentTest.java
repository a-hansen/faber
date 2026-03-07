package com.comfortanalytics.faber.agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import com.comfortanalytics.faber.routing.AgentRole;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaseAgentTest {

    @TempDir
    Path tempDir;

    @Test
    void handleDelegatesToTheExecutionEngineAndBuildsMessages() throws Exception {
        RecordingExecutionEngine executionEngine = new RecordingExecutionEngine("agent-response");
        TestAgent agent = new TestAgent(tempDir, executionEngine, AgentRole.CONTEXT_CONDENSER, ModelTier.TIER3_POWERFUL);
        TaskRequest request = new TaskRequest(
                "req-1",
                "Summarize this conversation.",
                Map.of(),
                Instant.parse("2026-03-07T00:00:00Z"));

        assertEquals("agent-response", agent.handle(request));
        assertEquals(AgentRole.CONTEXT_CONDENSER, agent.role());
        assertEquals(ModelTier.TIER3_POWERFUL, agent.modelTier());
        assertEquals(ModelTier.TIER3_POWERFUL, executionEngine.lastRequest().modelTier());
        assertEquals(
                "Agent role: CONTEXT_CONDENSER"
                        + System.lineSeparator()
                        + "Instructions: Condense the conversation into a short summary.",
                executionEngine.lastRequest().systemMessage());
        assertEquals("Summarize this conversation.", executionEngine.lastRequest().userMessage());
    }

    @Test
    void handleCanOverrideTheDefaultModelTier() throws Exception {
        RecordingExecutionEngine executionEngine = new RecordingExecutionEngine("agent-response");
        TestAgent agent = new TestAgent(tempDir, executionEngine, AgentRole.JAVA_DEVELOPER, ModelTier.TIER2_BALANCED);
        TaskRequest request = new TaskRequest(
                "req-1",
                "Handle this complex request.",
                Map.of(),
                Instant.parse("2026-03-07T00:00:00Z"));

        agent.handle(request, ModelTier.TIER3_POWERFUL);

        assertEquals(ModelTier.TIER3_POWERFUL, executionEngine.lastRequest().modelTier());
    }

    @Test
    void rejectsNullExecutionEngines() {
        assertThrows(
                NullPointerException.class,
                () -> new TestAgent(tempDir, null, AgentRole.JAVA_DEVELOPER, ModelTier.TIER2_BALANCED));
    }

    @Test
    void rejectsNullRequests() {
        RecordingExecutionEngine executionEngine = new RecordingExecutionEngine("agent-response");
        TestAgent agent = new TestAgent(tempDir, executionEngine, AgentRole.JAVA_DEVELOPER, ModelTier.TIER2_BALANCED);

        assertThrows(NullPointerException.class, () -> agent.handle(null));
    }

    private static final class TestAgent extends BaseAgent {

        private TestAgent(Path workspaceRoot, AgentExecutionEngine executionEngine, AgentRole role, ModelTier modelTier) {
            super(
                    role,
                    modelTier,
                    executionEngine,
                    new WorkspaceCodeMapLoader(workspaceRoot),
                    new WorkspaceMapService(workspaceRoot));
        }

        @Override
        protected String instructions() {
            return "Condense the conversation into a short summary.";
        }
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
