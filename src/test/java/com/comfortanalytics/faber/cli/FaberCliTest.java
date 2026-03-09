package com.comfortanalytics.faber.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.comfortanalytics.faber.agents.DynamicAgent;
import com.comfortanalytics.faber.cli.config.FaberConfig;
import com.comfortanalytics.faber.cli.config.ModelsConfig;
import com.comfortanalytics.faber.cli.config.ProviderConfig;
import com.comfortanalytics.faber.cli.config.RoutingConfig;
import com.comfortanalytics.faber.cli.config.WorkspaceConfig;
import com.comfortanalytics.faber.orchestrator.OrchestratorAgent;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import com.comfortanalytics.faber.routing.RuleBasedRoutingStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FaberCliTest {

    @TempDir
    Path tempDir;

    @Test
    void printsUsageWhenHelpIsRequested() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        FaberCli cli = new FaberCli(
                new ConfigLoader(),
                Map.of(),
                new PrintStream(stdout),
                new PrintStream(stderr),
                (config, environment, configDirectory) -> {
                    throw new AssertionError("bootstrapper should not be called for --help");
                });

        int exitCode = cli.run(new String[] {"--help"});

        assertEquals(0, exitCode);
        assertTrue(stdout.toString().contains("Usage:"));
        assertEquals("", stderr.toString());
    }

    @Test
    void readsTheTaskFileAndPrintsTheAgentResponse() throws Exception {
        Path configPath = tempDir.resolve("faber.yml");
        Files.writeString(configPath, """
                workspace:
                  rootPath: .
                routing:
                  mode: rule_based
                models:
                  tier1:
                    primary:
                      provider: gemini
                      model: gemini-2.0-flash
                  tier2:
                    primary:
                      provider: openai
                      model: gpt-4.1-mini
                """);
        Path taskPath = tempDir.resolve("task.md");
        Files.writeString(taskPath, "Refactor the parser.");
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        FaberCli cli = new FaberCli(
                new ConfigLoader(),
                Map.of(),
                new PrintStream(stdout),
                new PrintStream(stderr),
                (config, environment, configDirectory) -> new OrchestratorAgent(
                        new RuleBasedRoutingStrategy(),
                        decision -> new DynamicAgent(
                                decision.role(),
                                decision.modelTier(),
                                decision.persona(),
                                request -> "cli-response",
                                List.of(),
                                "")));

        int exitCode = cli.run(new String[] {"--config", configPath.toString(), "--task", taskPath.toString()});

        assertEquals(0, exitCode);
        assertEquals("cli-response" + System.lineSeparator(), stdout.toString());
        assertEquals("", stderr.toString());
    }

    @Test
    void bootstrapsTheRealRuleBasedRuntimeWithFakeChatModels() throws Exception {
        Files.writeString(tempDir.resolve("CODE_MAP.md"), "# CODE_MAP\n\n### DynamicAgent\n- signature");
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("BootstrappedType.java"), """
                package com.example;

                public final class BootstrappedType {

                    public String value() {
                        return "ok";
                    }
                }
                """);
        FaberConfig config = new FaberConfig(
                new WorkspaceConfig("."),
                new RoutingConfig("RULE_BASED"),
                new ModelsConfig(
                        Map.of("router", new ProviderConfig("gemini", "gemini-2.0-flash")),
                        Map.of("executor", new ProviderConfig("openai", "gpt-4.1-mini"))));

        OrchestratorAgent orchestrator = FaberCli.bootstrapRuntime(
                config,
                Map.of(),
                tempDir,
                (providerId, providerConfig, environment, listeners) -> new StaticChatModel("boot-response", listeners));
        TaskRequest request = new TaskRequest(
                "req-1",
                "Write a Java method to parse JSON.",
                Map.of("source", "test"),
                Instant.parse("2026-03-08T00:00:00Z"));

        String response = orchestrator.execute(request);
        Path transcriptFile = FaberCli.auditTranscriptFile(tempDir, "tier2.executor");
        String transcript = Files.readString(transcriptFile);

        assertEquals("boot-response", response);
        assertTrue(Files.exists(transcriptFile));
        assertTrue(transcript.contains("\"agent\":\"tier2.executor\""));
        assertTrue(transcript.contains("boot-response"));
        assertTrue(transcript.contains("Write a Java method to parse JSON."));
    }

    private static final class StaticChatModel implements ChatModel {

        private final String response;
        private final List<ChatModelListener> listeners;

        private StaticChatModel(String response, List<ChatModelListener> listeners) {
            this.response = response;
            this.listeners = List.copyOf(listeners);
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            ChatResponse responseContext = ChatResponse.builder()
                    .aiMessage(AiMessage.from(response))
                    .build();
            ChatModelResponseContext context = new ChatModelResponseContext(responseContext, request, null, Map.of());
            for (ChatModelListener listener : listeners) {
                listener.onResponse(context);
            }
            return responseContext;
        }
    }
}
