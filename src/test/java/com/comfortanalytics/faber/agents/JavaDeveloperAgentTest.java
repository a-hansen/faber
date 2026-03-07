package com.comfortanalytics.faber.agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.comfortanalytics.faber.model.ModelProviderManager;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import com.comfortanalytics.faber.routing.AgentRole;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaDeveloperAgentTest {

    @TempDir
    Path tempDir;

    @Test
    void exposesItsFixedRoleAndDefaultTier() {
        RecordingExecutionEngine executionEngine = new RecordingExecutionEngine("agent-response");
        JavaDeveloperAgent agent = new JavaDeveloperAgent(tempDir, executionEngine, List.of());

        assertEquals(AgentRole.JAVA_DEVELOPER, agent.role());
        assertEquals(ModelTier.TIER2_BALANCED, agent.modelTier());
    }

    @Test
    void buildsAJavaFocusedPrompt() throws TimeoutException {
        AgentTestSupport.CapturingModelProvider provider =
                new AgentTestSupport.CapturingModelProvider("gemini", "gemini-pro", "java-response");
        ModelProviderManager manager = AgentTestSupport.managerForTier(ModelTier.TIER2_BALANCED, provider);
        JavaDeveloperAgent agent = new JavaDeveloperAgent(manager);
        TaskRequest request = new TaskRequest(
                "req-1",
                "Write a Java method to parse JSON.",
                Map.of(),
                Instant.parse("2026-03-07T00:00:00Z"));

        assertEquals("java-response", agent.handle(request));
        assertTrue(provider.lastPrompt().contains("Java developer agent"));
        assertTrue(provider.lastPrompt().contains("maintainable Java code"));
        assertTrue(provider.lastPrompt().contains("Write a Java method to parse JSON."));
    }

    @Test
    void injectsCodeMapAndWorkspaceMapIntoTheSystemMessageAndReloadsThemDynamically() throws Exception {
        Files.writeString(tempDir.resolve("CODE_MAP.md"), "# CODE_MAP\n\nJavaDeveloperAgent\nmethodOne()");
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("WorkspaceIndexedType.java"), """
                package com.example;

                public final class WorkspaceIndexedType {

                    public WorkspaceIndexedType() {
                    }

                    public String hello(String name) {
                        return name;
                    }
                }
                """);
        RecordingExecutionEngine executionEngine = new RecordingExecutionEngine("java-response");
        JavaDeveloperAgent agent = new JavaDeveloperAgent(tempDir, executionEngine, List.of("tool-a", "tool-b"));
        TaskRequest request = new TaskRequest(
                "req-1",
                "Write a Java method to parse JSON.",
                Map.of(),
                Instant.parse("2026-03-07T00:00:00Z"));

        assertEquals("java-response", agent.handle(request));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Current codebase architecture"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("JavaDeveloperAgent"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Current workspace map"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("Package com.example"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("WorkspaceIndexedType (class)"));
        assertEquals(2, executionEngine.lastRequest().tools().size());

        Files.writeString(tempDir.resolve("CODE_MAP.md"), "# CODE_MAP\n\nJavaDeveloperAgent\nmethodTwo()");
        Files.writeString(srcDir.resolve("WorkspaceIndexedType.java"), """
                package com.example;

                public final class WorkspaceIndexedType {

                    public WorkspaceIndexedType() {
                    }

                    public String changed(String name) {
                        return name;
                    }
                }
                """);
        agent.handle(request);

        assertTrue(executionEngine.lastRequest().systemMessage().contains("methodTwo()"));
        assertTrue(executionEngine.lastRequest().systemMessage().contains("public String changed(String name)"));
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
