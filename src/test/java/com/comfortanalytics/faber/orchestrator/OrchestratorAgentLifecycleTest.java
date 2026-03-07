package com.comfortanalytics.faber.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.comfortanalytics.faber.agents.AgentExecutionEngine;
import com.comfortanalytics.faber.agents.AgentExecutionRequest;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.routing.DynamicRoutingStrategy;
import com.comfortanalytics.faber.routing.RoutingDecision;
import com.comfortanalytics.faber.tools.GradleExecutionService;
import com.comfortanalytics.faber.tools.SandboxedFileService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OrchestratorAgentLifecycleTest {

    @TempDir
    Path tempDir;

    @Test
    void executesTheSelectedAgentWithInjectedToolsAndDynamicCodeMapContext() throws Exception {
        Files.writeString(tempDir.resolve("CODE_MAP.md"), "# CODE_MAP\n\n### JavaDeveloperAgent\n- signature");
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("OrchestratedType.java"), """
                package com.example;

                public final class OrchestratedType {

                    public OrchestratedType() {
                    }

                    public int run() {
                        return 1;
                    }
                }
                """);
        RecordingExecutionEngine executionEngine = new RecordingExecutionEngine("agent-response");
        SandboxedFileService fileService = new SandboxedFileService(tempDir, SandboxedFileService.Mode.READ_WRITE);
        GradleExecutionService gradleExecutionService = new GradleExecutionService(tempDir);
        DefaultAgentFactory agentFactory = new DefaultAgentFactory(tempDir, executionEngine, fileService, gradleExecutionService);
        DynamicRoutingStrategy routingStrategy = new DynamicRoutingStrategy(
                request -> new RoutingDecision(
                        com.comfortanalytics.faber.routing.AgentRole.JAVA_DEVELOPER,
                        ModelTier.TIER3_POWERFUL));
        OrchestratorAgent orchestrator = new OrchestratorAgent(routingStrategy, agentFactory);
        TaskRequest request = new TaskRequest(
                "req-1",
                "Write a Java method to parse JSON.",
                Map.of(),
                Instant.parse("2026-03-07T00:00:00Z"));

        String response = orchestrator.execute(request);

        assertEquals("agent-response", response);
        assertEquals(ModelTier.TIER3_POWERFUL, executionEngine.lastRequest().modelTier());
        assertEquals(2, executionEngine.lastRequest().tools().size());
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Current codebase architecture"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("JavaDeveloperAgent"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Current workspace map"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Package com.example"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("OrchestratedType (class)"));
        assertEquals("Write a Java method to parse JSON.", executionEngine.lastRequest().userMessage());
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
